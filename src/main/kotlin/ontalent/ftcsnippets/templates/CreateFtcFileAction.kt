/** CreateFtcFileAction.kt */

package ontalent.ftcsnippets.templates

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager

/**
 * Base action for the `New -> FTC` group. Asks for a class name and flavor,
 * renders the matching template and drops the finished file into the directory
 * the user right-clicked.
 */
abstract class CreateFtcFileAction(private val kind: FtcTemplateKind) :
    AnAction(kind.displayName, kind.description, null) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.project != null && e.getData(LangDataKeys.IDE_VIEW) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val view = e.getData(LangDataKeys.IDE_VIEW) ?: return
        val directory = view.orChooseDirectory ?: return

        val targetPackage = FtcPackages.resolve(directory)

        val dialog = NewFtcFileDialog(project, kind, targetPackage)
        if (!dialog.showAndGet()) return

        val fileName = "${dialog.className}.${dialog.language.extension}"
        if (directory.virtualFile.findChild(fileName) != null) {
            Messages.showErrorDialog(
                project,
                "'$fileName' already exists in ${directory.virtualFile.presentableUrl}.",
                "Cannot Create File"
            )
            return
        }

        val content = FtcFileTemplates.render(
            kind = kind,
            className = dialog.className,
            packageName = targetPackage,
            language = dialog.language,
            framework = dialog.framework,
            pattern = dialog.pattern
        )

        createFile(project, directory, fileName, content)
    }

    private fun createFile(
        project: Project,
        directory: PsiDirectory,
        fileName: String,
        content: String
    ) {
        try {
            WriteCommandAction.runWriteCommandAction(project) {
                val created = directory.virtualFile.createChildData(this, fileName)
                VfsUtil.saveText(created, content)

                // The template text is already formatted; this only nudges it into the
                // project's own code style, so a failure here is not worth reporting.
                try {
                    PsiManager.getInstance(project).findFile(created)?.let { psiFile ->
                        CodeStyleManager.getInstance(project).reformat(psiFile)
                    }
                } catch (_: Exception) {
                }

                FileEditorManager.getInstance(project).openFile(created, true)
            }
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                "Could not create '$fileName': ${ex.message}",
                "Cannot Create File"
            )
        }
    }
}

class CreateAutonomousOpModeAction : CreateFtcFileAction(FtcTemplateKind.AUTONOMOUS)

class CreateTeleOpOpModeAction : CreateFtcFileAction(FtcTemplateKind.TELEOP)

class CreateSubsystemAction : CreateFtcFileAction(FtcTemplateKind.SUBSYSTEM)

class CreateCommandAction : CreateFtcFileAction(FtcTemplateKind.COMMAND)

class CreateRobotContainerAction : CreateFtcFileAction(FtcTemplateKind.ROBOT_CONTAINER)
