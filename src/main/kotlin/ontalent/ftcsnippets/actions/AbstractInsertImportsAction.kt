package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager

abstract class AbstractInsertImportsAction(
    text: String,
    description: String
) : AnAction(text, description, null) {

    final override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        val javaFile = psiFile as? PsiJavaFile
        if (javaFile != null) {
            insertIntoJavaFile(project, javaFile)
            return
        }

        // Kotlin files are handled as plain text, so the plugin does not have to
        // depend on the Kotlin plugin's PSI API.
        if (psiFile.virtualFile?.extension.equals("kt", ignoreCase = true)) {
            val document = e.getData(CommonDataKeys.EDITOR)?.document ?: return
            insertIntoKotlinFile(project, psiFile, document)
        }
    }

    private fun insertIntoJavaFile(project: Project, psiFile: PsiJavaFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            val elementFactory = JavaPsiFacade.getElementFactory(project)

            // 1. Handle Package Declaration
            val correctPackage = getCorrectPackageFromPath(psiFile)
            if (correctPackage != null) {
                val packageStatement = psiFile.packageStatement
                if (packageStatement == null) {
                    val newPackageStatement = elementFactory.createPackageStatement(correctPackage)
                    psiFile.addBefore(newPackageStatement, psiFile.firstChild)
                } else if (packageStatement.packageName != correctPackage) {
                    val newPackageStatement = elementFactory.createPackageStatement(correctPackage)
                    packageStatement.replace(newPackageStatement)
                }
            }

            // 2. Handle Import Insertion
            val importList = psiFile.importList
            if (importList != null) {
                for (importPath in importedNames()) {
                    if (importList.findSingleImportStatement(importPath) == null) {
                        try {
                            val psiClass = JavaPsiFacade.getInstance(project)
                                .findClass(importPath, psiFile.resolveScope)

                            val statement = if (psiClass != null) {
                                elementFactory.createImportStatement(psiClass)
                            } else {
                                val ref = elementFactory.createReferenceElementByFQClassName(
                                    importPath, psiFile.resolveScope
                                )
                                elementFactory.createImportStatement(ref.element as? PsiClass ?: continue)
                            }
                            importList.add(statement)
                        } catch (ex: Exception) {
                            continue
                        }
                    }
                }
            }

            // 3. Reformat
            try {
                CodeStyleManager.getInstance(project).reformat(psiFile)
            } catch (ex: Exception) {
            }
        }
    }

    private fun insertIntoKotlinFile(project: Project, psiFile: PsiFile, document: Document) {
        WriteCommandAction.runWriteCommandAction(project) {
            // 1. Handle Package Declaration
            if (!PACKAGE_REGEX.containsMatchIn(document.text)) {
                val correctPackage = getCorrectPackageFromPath(psiFile)
                if (correctPackage != null) {
                    document.insertString(0, "package $correctPackage\n\n")
                }
            }

            // 2. Handle Import Insertion
            val currentText = document.text
            val missing = importedNames().filterNot { fqName ->
                Regex("""^\s*import\s+${Regex.escape(fqName)}\s*;?\s*$""", RegexOption.MULTILINE)
                    .containsMatchIn(currentText)
            }
            if (missing.isNotEmpty()) {
                val block = missing.joinToString("\n") { "import $it" }
                document.insertString(findImportInsertPosition(document), "$block\n")
            }

            // 3. Reformat
            try {
                CodeStyleManager.getInstance(project).reformatText(psiFile, 0, document.textLength)
            } catch (ex: Exception) {
            }
        }
    }

    /** The fully qualified names declared by [getInsertText], stripped of syntax. */
    private fun importedNames(): List<String> =
        getInsertText().split("\n")
            .map { it.trim() }
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").removeSuffix(";").trim() }

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

    protected abstract fun getInsertText(): String

    private fun getCorrectPackageFromPath(psiFile: PsiFile): String? {
        val virtualFile = psiFile.virtualFile ?: return null
        val filePath = virtualFile.path.replace('\\', '/')
        val basePackage = "org.firstinspires.ftc.teamcode"

        val patterns = listOf("/teamcode/", "/TeamCode/")
        var teamcodeIndex = -1
        var foundPattern = ""

        for (pattern in patterns) {
            val index = filePath.indexOf(pattern)
            if (index != -1) {
                teamcodeIndex = index
                foundPattern = pattern
                break
            }
        }

        // File is not inside a teamcode folder at all
        if (teamcodeIndex == -1) return basePackage

        // Everything after /teamcode/ up to the filename
        val afterTeamcode = filePath.substring(teamcodeIndex + foundPattern.length)
        val lastSlash = afterTeamcode.lastIndexOf('/')

        return if (lastSlash == -1) {
            // File is directly in teamcode/
            basePackage
        } else {
            // File is in a subfolder e.g. teamcode/autonomous/
            val subPath = afterTeamcode.substring(0, lastSlash).trim('/')
            "$basePackage.${subPath.replace('/', '.')}"
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isVisible = true
        e.presentation.isEnabled = editor != null
    }

    private companion object {
        val PACKAGE_REGEX = Regex("""^\s*package\s+[\w.]+\s*;?""", RegexOption.MULTILINE)
        val IMPORT_REGEX = Regex("""^\s*import\s+[\w.*]+\s*;?""", RegexOption.MULTILINE)
    }
}
