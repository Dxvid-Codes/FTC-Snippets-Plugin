package ontalent.ftcsnippets.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction

class FtcErrorInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitMethod(method: PsiMethod) {
                checkMissingWaitForStart(method, holder)
                checkMissingTelemetryUpdate(method, holder)
                checkGamepadIssues(method, holder)
            }

            override fun visitField(field: PsiField) {
                checkUninitializedHardware(field, holder)
            }
        }
    }

    private fun checkMissingWaitForStart(method: PsiMethod, holder: ProblemsHolder) {
        if (method.name == "runOpMode" && isFtcOpMode(method.containingClass)) {
            val bodyText = method.body?.text ?: ""
            if (!bodyText.contains("waitForStart")) {
                holder.registerProblem(
                    method.nameIdentifier ?: method,
                    "Missing waitForStart() - code may run before match starts",
                    ProblemHighlightType.WARNING,
                    InsertWaitForStartFix()
                )
            }
        }
    }

    private fun checkMissingTelemetryUpdate(method: PsiMethod, holder: ProblemsHolder) {
        if (isFtcOpMode(method.containingClass)) {
            val bodyText = method.body?.text ?: ""
            if (bodyText.contains("telemetry.addData") && !bodyText.contains("telemetry.update")) {
                holder.registerProblem(
                    method.nameIdentifier ?: method,
                    "Missing telemetry.update() - data won't appear on Driver Station",
                    ProblemHighlightType.WARNING,
                    InsertTelemetryUpdateFix()
                )
            }
        }
    }

    private fun checkGamepadIssues(method: PsiMethod, holder: ProblemsHolder) {
        if (isFtcOpMode(method.containingClass)) {
            val body = method.body ?: return
            body.statements.forEach { statement ->
                val text = statement.text
                if ((text.contains("gamepad1") || text.contains("gamepad2")) &&
                    text.contains("press") &&
                    !text.contains("if")) {
                    holder.registerProblem(
                        statement,
                        "Gamepad button check should be in an if statement",
                        ProblemHighlightType.WEAK_WARNING
                    )
                }
            }
        }
    }

    private fun checkUninitializedHardware(field: PsiField, holder: ProblemsHolder) {
        val containingClass = field.containingClass ?: return
        if (!isFtcOpMode(containingClass)) return

        val fieldType = field.type.canonicalText
        val fieldName = field.name ?: return

        val ftcHardwareTypes = listOf(
            "com.qualcomm.robotcore.hardware.DcMotor",
            "com.qualcomm.robotcore.hardware.Servo",
            "com.qualcomm.robotcore.hardware.CRServo"
        )

        val isFtcHardware = ftcHardwareTypes.any { fieldType.contains(it) }

        if (isFtcHardware) {
            // Better initialization check - look for hardwareMap.get with this field name
            val isInitialized = containingClass.methods.any { method ->
                method.body?.text?.contains("$fieldName\\s*=\\s*hardwareMap\\.get".toRegex()) == true
            }

            if (!isInitialized) {
                holder.registerProblem(
                    field.nameIdentifier ?: field,
                    "Hardware device '$fieldName' not initialized with hardwareMap",
                    ProblemHighlightType.ERROR,
                    InitializeHardwareFix(fieldType, fieldName)
                )
            }
        }
    }

    private fun isFtcOpMode(psiClass: PsiClass?): Boolean {
        if (psiClass == null) return false
        return psiClass.superTypes.any { superType ->
            superType.presentableText.contains("LinearOpMode") ||
                    superType.presentableText.contains("OpMode")
        }
    }

    // Quick Fix for missing waitForStart
    private class InsertWaitForStartFix : LocalQuickFix {
        override fun getName() = "Insert waitForStart()"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) {
                val method = descriptor.psiElement as? PsiMethod ?: return@runWriteCommandAction
                val factory = JavaPsiFacade.getElementFactory(project)

                try {
                    val waitForStartStmt = factory.createStatementFromText("waitForStart();", method)
                    val body = method.body ?: return@runWriteCommandAction

                    // Insert at the beginning of the method body
                    body.addAfter(waitForStartStmt, body.firstChild)
                } catch (e: Exception) {
                    // Fallback: try to insert at the start
                    try {
                        val waitForStartStmt = factory.createStatementFromText("waitForStart();", method)
                        method.body?.add(waitForStartStmt)
                    } catch (e2: Exception) {
                        // If all fails, do nothing
                    }
                }
            }
        }
    }

    // Quick Fix for missing telemetry.update()
    private class InsertTelemetryUpdateFix : LocalQuickFix {
        override fun getName() = "Insert telemetry.update()"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) {
                val method = descriptor.psiElement as? PsiMethod ?: return@runWriteCommandAction
                val factory = JavaPsiFacade.getElementFactory(project)

                try {
                    val updateStmt = factory.createStatementFromText("telemetry.update();", method)
                    method.body?.add(updateStmt)
                } catch (e: Exception) {
                    // If fails, do nothing
                }
            }
        }
    }

    // Quick Fix for uninitialized hardware - FIXED VERSION
    private class InitializeHardwareFix(
        private val type: String,
        private val name: String
    ) : LocalQuickFix {
        override fun getName() = "Initialize $name with hardwareMap"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) {
                val field = descriptor.psiElement as? PsiField ?: return@runWriteCommandAction
                val containingClass = field.containingClass ?: return@runWriteCommandAction

                // Find the best method to insert into
                val runOpModeMethod = containingClass.findMethodsByName("runOpMode", false).firstOrNull()
                val initMethod = containingClass.findMethodsByName("init", false).firstOrNull()
                val method = runOpModeMethod ?: initMethod ?: return@runWriteCommandAction

                val factory = JavaPsiFacade.getElementFactory(project)

                // Extract short type name (e.g., "DcMotor" from full path)
                val shortType = if (type.contains('.')) {
                    type.substringAfterLast('.')
                } else {
                    type
                }

                try {
                    // Create the initialization statement
                    val initStmt = factory.createStatementFromText(
                        "$name = hardwareMap.get($shortType.class, \"$name\");",
                        method
                    )

                    val body = method.body ?: return@runWriteCommandAction

                    // Insert after the first statement in the method body
                    val firstStmt = body.statements.firstOrNull()
                    if (firstStmt != null) {
                        body.addAfter(initStmt, firstStmt)
                    } else {
                        body.add(initStmt)
                    }
                } catch (e: Exception) {
                    // Try alternative syntax if first attempt fails
                    try {
                        val initStmt = factory.createStatementFromText(
                            "$name = hardwareMap.get($shortType.class, \"$name\");",
                            method
                        )
                        method.body?.add(initStmt)
                    } catch (e2: Exception) {
                        // Final fallback
                    }
                }
            }
        }
    }
}