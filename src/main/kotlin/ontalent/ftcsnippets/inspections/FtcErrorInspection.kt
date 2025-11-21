//FTCErrorInspection

package com.ontalent.ftcsnippets.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.intellij.openapi.project.Project

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
            val isInitialized = containingClass.methods.any { method ->
                method.body?.text?.contains("hardwareMap.*$fieldName") == true
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
            val method = descriptor.psiElement as? PsiMethod ?: return
            val factory = JavaPsiFacade.getElementFactory(project)
            val waitForStartStmt = factory.createStatementFromText("waitForStart();", method)

            method.body?.addAfter(waitForStartStmt, method.body?.firstChild)
        }
    }

    // Quick Fix for missing telemetry.update()
    private class InsertTelemetryUpdateFix : LocalQuickFix {
        override fun getName() = "Insert telemetry.update()"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val method = descriptor.psiElement as? PsiMethod ?: return
            val factory = JavaPsiFacade.getElementFactory(project)
            val updateStmt = factory.createStatementFromText("telemetry.update();", method)

            method.body?.add(updateStmt)
        }
    }

    // Quick Fix for uninitialized hardware
    private class InitializeHardwareFix(
        private val type: String,
        private val name: String
    ) : LocalQuickFix {
        override fun getName() = "Initialize $name with hardwareMap"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val field = descriptor.psiElement as? PsiField ?: return
            val containingClass = field.containingClass ?: return

            val runOpModeMethod = containingClass.findMethodsByName("runOpMode", false).firstOrNull()
            val method = runOpModeMethod ?: containingClass.findMethodsByName("init", false).firstOrNull() ?: return

            val factory = JavaPsiFacade.getElementFactory(project)
            val shortType = type.substringAfterLast('.')
            val initStmt = factory.createStatementFromText(
                "$name = hardwareMap.get($shortType.class, \"$name\");",
                method
            )

            method.body?.addAfter(initStmt, method.body?.firstChild)
        }
    }
}