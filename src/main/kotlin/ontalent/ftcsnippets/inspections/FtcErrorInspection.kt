package ontalent.ftcsnippets.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil

class FtcErrorInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitClass(aClass: PsiClass) {
                if (isFtcOpModeClass(aClass)) {
                    checkClassForErrors(aClass, holder)
                }
            }
        }
    }

    private fun checkClassForErrors(clazz: PsiClass, holder: ProblemsHolder) {
        // Check all fields for uninitialized hardware
        clazz.fields.forEach { field ->
            checkUninitializedHardware(field, holder)
        }

        // Check all methods for common errors
        clazz.methods.forEach { method ->
            checkMethodForErrors(method, holder)
        }
    }

    private fun checkMethodForErrors(method: PsiMethod, holder: ProblemsHolder) {
        if (method.name == "runOpMode") {
            checkMissingWaitForStart(method, holder)
        }

        checkMissingTelemetryUpdate(method, holder)
        checkGamepadIssues(method, holder)
    }

    private fun checkMissingWaitForStart(method: PsiMethod, holder: ProblemsHolder) {
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

    private fun checkMissingTelemetryUpdate(method: PsiMethod, holder: ProblemsHolder) {
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

    private fun checkGamepadIssues(method: PsiMethod, holder: ProblemsHolder) {
        val body = method.body ?: return
        body.statements.forEach { statement ->
            val text = statement.text
            if ((text.contains("gamepad1") || text.contains("gamepad2")) &&
                text.contains("press") &&
                !text.contains("if\\s*\\(")) {
                holder.registerProblem(
                    statement,
                    "Gamepad button check should be in an if statement",
                    ProblemHighlightType.WEAK_WARNING
                )
            }
        }
    }

    private fun checkUninitializedHardware(field: PsiField, holder: ProblemsHolder) {
        val fieldName = field.name ?: return
        val fieldType = field.type.canonicalText

        // Check if this is an FTC hardware type
        if (isFtcHardwareType(fieldType)) {
            val containingClass = field.containingClass ?: return

            // Check if the field is initialized anywhere in the class
            val isInitialized = isHardwareInitialized(containingClass, fieldName)

            if (!isInitialized) {
                holder.registerProblem(
                    field.nameIdentifier ?: field,
                    "Hardware device '$fieldName' not initialized with hardwareMap",
                    ProblemHighlightType.ERROR,
                    InitializeHardwareFix(fieldName, extractShortType(fieldType))
                )
            }
        }
    }

    private fun isFtcHardwareType(type: String): Boolean {
        return type.contains("DcMotor") ||
                type.contains("Servo") ||
                type.contains("CRServo") ||
                type.contains("DistanceSensor") ||
                type.contains("BNO055IMU") ||
                type.contains("IMU")
    }

    private fun isHardwareInitialized(clazz: PsiClass, fieldName: String): Boolean {
        // Look for hardwareMap.get calls with this field name
        val visitor = object : JavaRecursiveElementVisitor() {
            var found = false

            override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
                super.visitAssignmentExpression(expression)
                val lhs = expression.lExpression.text
                if (lhs == fieldName) {
                    val rhs = expression.rExpression?.text ?: ""
                    if (rhs.contains("hardwareMap.get")) {
                        found = true
                    }
                }
            }
        }

        clazz.accept(visitor)
        return visitor.found
    }

    private fun isFtcOpModeClass(psiClass: PsiClass?): Boolean {
        if (psiClass == null) return false

        // Check for LinearOpMode or OpMode in inheritance chain
        val superTypes = psiClass.supers
        if (superTypes.any { it.qualifiedName?.contains("LinearOpMode") == true ||
                    it.qualifiedName?.contains("OpMode") == true }) {
            return true
        }

        // Also check if class has @TeleOp or @Autonomous annotation
        val annotations = psiClass.modifierList?.annotations ?: emptyArray()
        return annotations.any {
            it.qualifiedName?.contains("TeleOp") == true ||
                    it.qualifiedName?.contains("Autonomous") == true
        }
    }

    private fun extractShortType(fullType: String): String {
        return fullType.substringAfterLast('.', fullType)
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

                    // Insert at the beginning
                    val firstStatement = body.statements.firstOrNull()
                    if (firstStatement != null) {
                        body.addBefore(waitForStartStmt, firstStatement)
                    } else {
                        body.add(waitForStartStmt)
                    }
                } catch (e: Exception) {
                    // Ignore
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
                    // Ignore
                }
            }
        }
    }

    // Quick Fix for uninitialized hardware
    private class InitializeHardwareFix(
        private val fieldName: String,
        private val typeName: String
    ) : LocalQuickFix {
        override fun getName() = "Initialize $fieldName with hardwareMap"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    val field = descriptor.psiElement as? PsiField ?: return@runWriteCommandAction
                    val containingClass = field.containingClass ?: return@runWriteCommandAction

                    // Find runOpMode method
                    val runOpModeMethod = containingClass.findMethodsByName("runOpMode", false).firstOrNull()
                    val method = runOpModeMethod ?: return@runWriteCommandAction

                    val body = method.body ?: return@runWriteCommandAction
                    val factory = JavaPsiFacade.getElementFactory(project)

                    // Create initialization statement
                    val initStatement = factory.createStatementFromText(
                        "$fieldName = hardwareMap.get($typeName.class, \"$fieldName\");",
                        method
                    )

                    // Insert at beginning of method
                    val firstStatement = body.statements.firstOrNull()
                    if (firstStatement != null) {
                        body.addBefore(initStatement, firstStatement)
                    } else {
                        body.add(initStatement)
                    }
                } catch (e: Exception) {
                    // Ignore - let the user know if needed
                }
            }
        }
    }
}