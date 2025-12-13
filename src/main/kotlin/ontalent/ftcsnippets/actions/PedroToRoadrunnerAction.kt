package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiExpression

class PedroToRoadrunnerAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // Collect all Pedro Pose expressions
        val pedroExpressions = mutableListOf<PsiNewExpression>()
        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: com.intellij.psi.PsiElement) {
                super.visitElement(element)
                if (element is PsiNewExpression) {
                    val type = element.classReference?.qualifiedName
                    if (type?.endsWith("Pose") == true) {
                        val args = element.argumentList?.expressions
                        if (args != null && args.size == 3) {
                            pedroExpressions.add(element)
                        }
                    }
                }
            }
        })

        if (pedroExpressions.isEmpty()) {
            return
        }

        // Perform rewrite inside write action
        WriteCommandAction.runWriteCommandAction(project) {
            val factory = JavaPsiFacade.getInstance(project).elementFactory
            val FIELD_HALF_SIZE = 72.0

            for (expr in pedroExpressions) {
                val args = expr.argumentList?.expressions ?: continue

                val x = args[0].text.toDoubleOrNull() ?: continue
                val y = args[1].text.toDoubleOrNull() ?: continue
                val headingDeg = args[2].text.toDoubleOrNull() ?: continue

                val rrX = x - FIELD_HALF_SIZE
                val rrY = y - FIELD_HALF_SIZE

                val rrHeading = Math.toRadians(headingDeg)
                val newExprText = "new Pose2d($rrX, $rrY, $rrHeading)"

                val newExpr = factory.createExpressionFromText(newExprText, expr)
                expr.replace(newExpr)
            }
        }
    }
}
