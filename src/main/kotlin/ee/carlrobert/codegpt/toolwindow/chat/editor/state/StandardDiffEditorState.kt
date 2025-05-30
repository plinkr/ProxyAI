package ee.carlrobert.codegpt.toolwindow.chat.editor.state

import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.application
import ee.carlrobert.codegpt.toolwindow.chat.editor.diff.DiffEditorManager
import ee.carlrobert.codegpt.toolwindow.chat.editor.header.DiffHeaderPanel
import ee.carlrobert.codegpt.toolwindow.chat.parser.ReplaceWaiting
import ee.carlrobert.codegpt.toolwindow.chat.parser.SearchReplace
import ee.carlrobert.codegpt.toolwindow.chat.parser.Segment

class StandardDiffEditorState(
    editor: EditorEx,
    segment: Segment,
    project: Project,
    diffViewer: UnifiedDiffViewer?,
    virtualFile: VirtualFile?,
    private val diffEditorManager: DiffEditorManager
) : DiffEditorState(editor, segment, project, diffViewer, virtualFile) {

    override fun applyAllChanges() {
        val changes = diffEditorManager.applyAllChanges()
        if (changes.isNotEmpty()) {
            (editor.permanentHeaderComponent as? DiffHeaderPanel)?.handleChangesApplied(changes)
            virtualFile?.let { OpenFileAction.openFile(it, project) }
        }
    }

    override fun updateContent(segment: Segment) {
        if (editor.editorKind == EditorKind.DIFF) {
            if (segment is SearchReplace) {
                diffEditorManager.updateDiffContent(segment.search, segment.replace)
                (editor.permanentHeaderComponent as? DiffHeaderPanel)
                    ?.updateDiffStats(diffViewer?.diffChanges ?: emptyList())
            } else if (segment is ReplaceWaiting) {
                diffEditorManager.updateDiffContent(segment.search, segment.replace)
                (editor.permanentHeaderComponent as? DiffHeaderPanel)
                    ?.updateDiffStats(diffViewer?.diffChanges ?: emptyList())
            }
        }
    }

    fun refresh() {
        application.executeOnPooledThread {
            runInEdt {
                diffViewer?.rediff(true)
            }
        }
    }
}
