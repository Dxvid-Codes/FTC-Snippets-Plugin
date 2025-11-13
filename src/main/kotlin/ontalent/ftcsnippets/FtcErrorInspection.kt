package ontalent.ftcsnippets

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.intellij.openapi.project.Project

class FtcErrorInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethod(method: PsiMethod) {
                val methodName = method.name

                // --- Detect missing waitForStart() ---
                if (methodName == "runOpMode" && method.body != null) {
                    val bodyText = method.body!!.text
                    if (!bodyText.contains("waitForStart()")) {
                        holder.registerProblem(
                            method.nameIdentifier ?: return,
                            "Missing waitForStart(). Your code may run before the match starts.",
                            InsertWaitForStartQuickFix()
                        )
                    }
                }

                // --- Detect telemetry.addData() without update() ---
                val body = method.body?.text ?: return
                if (body.contains("telemetry.addData") && !body.contains("telemetry.update")) {
                    holder.registerProblem(
                        method.nameIdentifier ?: return,
                        "Telemetry data won't appear without calling telemetry.update().",
                        InsertTelemetryUpdateQuickFix()
                    )
                }
            }

            override fun visitField(field: PsiField) {
                val fieldType = field.type.presentableText
                val name = field.name

                // --- Detect uninitialized FTC hardwareMap fields ---
                if (fieldType in listOf("DcMotor", "Servo", "BNO055IMU", "CRServo", "DistanceSensor")) {
                    val containingClass = field.containingClass ?: return
                    val methods = containingClass.methods

                    val isInitialized = methods.any { it.text.contains("hardwareMap.get") && it.text.contains(name) }
                    if (!isInitialized) {
                        holder.registerProblem(
                            field.nameIdentifier ?: return,
                            "Hardware device '$name' not initialized with hardwareMap.get().",
                            InsertHardwareMapQuickFix(fieldType, name)
                        )
                    }
                }
            }
        }
    }

    // Quick Fix: Insert waitForStart()
    private class InsertWaitForStartQuickFix : LocalQuickFix {
        override fun getFamilyName() = "Insert waitForStart()"
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val method = descriptor.psiElement.parent as? PsiMethod ?: return
            val factory = JavaPsiFacade.getElementFactory(project)
            val stmt = factory.createStatementFromText("waitForStart();", method)
            method.body?.addBefore(stmt, method.body?.statements?.firstOrNull())
        }
    }

    // Quick Fix: Insert telemetry.update()
    private class InsertTelemetryUpdateQuickFix : LocalQuickFix {
        override fun getFamilyName() = "Insert telemetry.update()"
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val method = descriptor.psiElement.parent as? PsiMethod ?: return
            val factory = JavaPsiFacade.getElementFactory(project)
            val stmt = factory.createStatementFromText("telemetry.update();", method)
            method.body?.add(stmt)
        }
    }

    // Quick Fix: Initialize hardware variable
    private class InsertHardwareMapQuickFix(
        private val type: String,
        private val name: String
    ) : LocalQuickFix {

        override fun getFamilyName() = "Initialize with hardwareMap.get()"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val field = descriptor.psiElement.parent as? PsiField ?: return
            val containingClass = field.containingClass ?: return
            val method = containingClass.findMethodsByName("runOpMode", false).firstOrNull() ?: return

            val factory = JavaPsiFacade.getElementFactory(project)
            val statement = factory.createStatementFromText(
                "$name = hardwareMap.get($type.class, \"$name\");",
                method
            )
            method.body?.addBefore(statement, method.body?.statements?.firstOrNull())
        }
    }
}
