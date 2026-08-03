/** AbstractInsertSkeletonAction.kt */

package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import ontalent.ftcsnippets.templates.FtcPackages

/**
 * Shared plumbing for the OpMode skeleton actions: package statement, de-duplicated
 * imports and the class body, in Java or Kotlin depending on the open file.
 */
abstract class AbstractInsertSkeletonAction(
    text: String,
    description: String
) : AnAction(text, description, null) {

    /** Fully qualified names, without the `import` keyword or a trailing semicolon. */
    protected abstract fun imports(kotlin: Boolean): List<String>

    protected abstract fun skeleton(className: String, kotlin: Boolean): String

    protected abstract val fallbackClassName: String

    final override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val document = editor.document

        val kotlin = psiFile.virtualFile?.extension.equals("kt", ignoreCase = true)
        val className = psiFile.virtualFile?.nameWithoutExtension ?: fallbackClassName
        val terminator = if (kotlin) "" else ";"

        WriteCommandAction.runWriteCommandAction(project) {
            if (!PACKAGE_REGEX.containsMatchIn(document.text)) {
                document.insertString(0, "package ${resolvePackage(psiFile)}$terminator\n\n")
            }

            // Only add what is not already imported, so running the action twice does
            // not leave a duplicated import block behind.
            val currentText = document.text
            val missing = imports(kotlin).filterNot { fqName ->
                Regex("""^\s*import\s+${Regex.escape(fqName)}\s*;?\s*$""", RegexOption.MULTILINE)
                    .containsMatchIn(currentText)
            }
            if (missing.isNotEmpty()) {
                val block = missing.joinToString("\n") { "import $it$terminator" }
                document.insertString(findImportInsertPosition(document), "$block\n")
            }

            document.insertString(document.textLength, "\n${skeleton(className, kotlin)}\n")

            try {
                CodeStyleManager.getInstance(project).reformatText(psiFile, 0, document.textLength)
            } catch (_: Exception) {
                // Non-fatal
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
        e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
    }

    private fun resolvePackage(psiFile: PsiFile): String {
        val parentPath = psiFile.virtualFile?.parent?.path
        return parentPath?.let { FtcPackages.fromDirectoryPath(it) } ?: DEFAULT_BASE_PACKAGE
    }

    private fun findImportInsertPosition(document: Document): Int {
        val text = document.text
        var offset = 0

        PACKAGE_REGEX.find(text)?.let { match ->
            offset = match.range.last + 1
            if (offset < text.length && text[offset] == '\n') offset++
        }

        IMPORT_REGEX.findAll(text).lastOrNull()?.let { match ->
            offset = match.range.last + 1
            if (offset < text.length && text[offset] == '\n') offset++
        }

        return offset.coerceAtMost(document.textLength)
    }

    private companion object {
        const val DEFAULT_BASE_PACKAGE = "org.firstinspires.ftc.teamcode"

        // Both languages: Kotlin has no trailing semicolon.
        val PACKAGE_REGEX = Regex("""^\s*package\s+[\w.]+\s*;?""", RegexOption.MULTILINE)
        val IMPORT_REGEX = Regex("""^\s*import\s+[\w.*]+\s*;?""", RegexOption.MULTILINE)
    }
}
