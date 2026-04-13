package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager

abstract class AbstractInsertImportsAction(
    text: String,
    description: String
) : AnAction(text, description, null) {

    final override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return

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
                val importsToAdd = getInsertText().split("\n")
                    .map { it.trim() }
                    .filter { it.startsWith("import ") }
                    .map { it.removePrefix("import ").removeSuffix(";").trim() }

                for (importPath in importsToAdd) {
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
}