package ee.carlrobert.codegpt.toolwindow.chat.editor.diff

import com.intellij.diff.tools.fragmented.UnifiedDiffChange
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.util.DiffUtil
import com.intellij.diff.util.Side
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diff.DiffBundle
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.util.concurrency.annotations.RequiresEdt
import ee.carlrobert.codegpt.toolwindow.chat.editor.diff.DiffManagerUtil.replaceContent
import ee.carlrobert.codegpt.toolwindow.chat.editor.header.DiffHeaderPanel

class DiffEditorManager(
    private val project: Project,
    private val diffViewer: UnifiedDiffViewer,
    private val virtualFile: VirtualFile?
) {

    fun updateDiffContent(searchContent: String, replaceContent: String): Boolean {
        val currentText = virtualFile?.readText() ?: return false
        val document = diffViewer.getDocument(Side.RIGHT)

        runInEdt {
            document.replaceContent(
                project,
                currentText.replaceFirst(searchContent.trim(), replaceContent.trim())
            )

            diffViewer.rediff(true)
            scrollToLastChange(diffViewer)
        }
        return true
    }

    fun applyAllChanges(): List<UnifiedDiffChange> {
        val document = diffViewer.getDocument(Side.LEFT)
        DiffManagerUtil.ensureDocumentWritable(project, document)

        val allChanges = mutableListOf<UnifiedDiffChange>()

        while (true) {
            val changes = diffViewer.diffChanges ?: break
            if (changes.isEmpty()) break

            val change = changes.first()

            DiffUtil.executeWriteCommand(
                document,
                project,
                DiffBundle.message("message.replace.change.command")
            ) {
                diffViewer.replaceChange(change, Side.RIGHT)
                diffViewer.scheduleRediff()
            }
            diffViewer.rediff(true)

            allChanges.add(change)
        }

        return allChanges
    }

    private fun scrollToLastChange(viewer: UnifiedDiffViewer) {
        val change = viewer.diffChanges?.lastOrNull() ?: return
        viewer.editors.firstOrNull()?.scrollingModel?.scrollTo(
            LogicalPosition(change.lineFragment.startLine2, 0),
            ScrollType.CENTER
        )
    }
}

object DiffManagerUtil {

    @RequiresEdt
    fun Document.replaceContent(project: Project, replaceContent: String) {
        ensureDocumentWritable(project, this)
        DiffUtil.executeWriteCommand(this, project, "Updating document") {
            setText(replaceContent)
        }
    }

    fun ensureDocumentWritable(project: Project, document: Document) {
        if (!document.isWritable) {
            DiffUtil.makeWritable(project, document)
            document.setReadOnly(false)
        }
    }
}