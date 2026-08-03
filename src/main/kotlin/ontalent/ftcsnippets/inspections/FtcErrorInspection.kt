package ontalent.ftcsnippets.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction

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
        checkMissingOpModeAnnotation(clazz, holder)

        // Check all fields for uninitialized hardware
        clazz.fields.forEach { field ->
            checkUninitializedHardware(field, holder)
        }

        // Check all methods for common errors
        clazz.methods.forEach { method ->
            checkMethodForErrors(method, holder)
        }

        // Expression level checks, scoped to this OpMode
        clazz.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                checkThreadSleep(expression, holder)
                checkOutOfRangeArgument(expression, holder)
            }

            override fun visitWhileStatement(statement: PsiWhileStatement) {
                super.visitWhileStatement(statement)
                checkLoopWithoutOpModeIsActive(statement, holder)
            }
        })
    }

    private fun checkMethodForErrors(method: PsiMethod, holder: ProblemsHolder) {
        if (method.name == "runOpMode") {
            checkMissingWaitForStart(method, holder)
        }

        checkMissingTelemetryUpdate(method, holder)
    }

    /**
     * An OpMode with no @TeleOp or @Autonomous annotation never shows up in the Driver
     * Station list, which looks exactly like a deploy that silently failed.
     */
    private fun checkMissingOpModeAnnotation(clazz: PsiClass, holder: ProblemsHolder) {
        if (clazz.hasModifierProperty(PsiModifier.ABSTRACT)) return
        if (clazz.name == null) return
        if (!extendsOpMode(clazz)) return

        val annotations = clazz.modifierList?.annotations ?: emptyArray()
        val hasRegistration = annotations.any { annotation ->
            val name = annotation.qualifiedName ?: return@any false
            name.endsWith("TeleOp") || name.endsWith("Autonomous") || name.endsWith("Disabled")
        }
        if (hasRegistration) return

        holder.registerProblem(
            clazz.nameIdentifier ?: clazz,
            "OpMode has no @TeleOp or @Autonomous annotation - it won't appear on the Driver Station",
            ProblemHighlightType.WARNING,
            AddOpModeAnnotationFix(TELEOP_ANNOTATION, "TeleOp"),
            AddOpModeAnnotationFix(AUTONOMOUS_ANNOTATION, "Autonomous")
        )
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

    /**
     * Thread.sleep() ignores the stop button, so the robot keeps moving after the match
     * ends. LinearOpMode.sleep() is interrupt aware.
     */
    private fun checkThreadSleep(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        val methodExpression = expression.methodExpression
        if (methodExpression.referenceName != "sleep") return
        if (methodExpression.qualifierExpression?.text != "Thread") return

        holder.registerProblem(
            expression,
            "Thread.sleep() ignores the stop button - use the OpMode's own sleep()",
            ProblemHighlightType.WARNING,
            ReplaceWithOpModeSleepFix()
        )
    }

    /** Motor power lives in [-1, 1] and servo position in [0, 1]; anything else is clipped. */
    private fun checkOutOfRangeArgument(expression: PsiMethodCallExpression, holder: ProblemsHolder) {
        val name = expression.methodExpression.referenceName ?: return
        val range = when (name) {
            "setPower" -> -1.0 to 1.0
            "setPosition" -> 0.0 to 1.0
            else -> return
        }

        val argument = expression.argumentList.expressions.singleOrNull() ?: return
        val evaluated = try {
            JavaPsiFacade.getInstance(expression.project)
                .constantEvaluationHelper
                .computeConstantExpression(argument)
        } catch (ex: Exception) {
            null
        }

        val value = (evaluated as? Number)?.toDouble() ?: return
        if (value >= range.first && value <= range.second) return

        holder.registerProblem(
            argument,
            "$name() expects a value between ${range.first} and ${range.second} - " +
                    "$value will be clipped",
            ProblemHighlightType.WARNING
        )
    }

    /**
     * A loop in runOpMode() that never consults opModeIsActive() keeps running after the
     * match is stopped, which is both a scoring and a safety problem.
     */
    private fun checkLoopWithoutOpModeIsActive(
        statement: PsiWhileStatement,
        holder: ProblemsHolder
    ) {
        val method = PsiTreeUtil.getParentOfType(statement, PsiMethod::class.java) ?: return
        if (method.name != "runOpMode") return

        val condition = statement.condition ?: return
        val conditionText = condition.text
        if (STOP_AWARE_CALLS.any { conditionText.contains(it) }) return

        holder.registerProblem(
            condition,
            "Loop does not check opModeIsActive() - it will keep running after the match is stopped",
            ProblemHighlightType.WARNING,
            AddOpModeIsActiveFix()
        )
    }

    private fun checkUninitializedHardware(field: PsiField, holder: ProblemsHolder) {
        val fieldName = field.name ?: return
        val fieldType = field.type.canonicalText

        // Check if this is an FTC hardware type
        if (isFtcHardwareType(fieldType)) {
            val containingClass = field.containingClass ?: return

            // Check if the field is initialized anywhere in the class
            val isInitialized = isHardwareInitialized(containingClass, field, fieldName)

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

    private fun isHardwareInitialized(clazz: PsiClass, field: PsiField, fieldName: String): Boolean {
        // Declared and assigned in one go: DcMotor motor = hardwareMap.get(...);
        if (field.initializer?.text?.contains("hardwareMap") == true) return true

        // Look for any assignment fed by the hardwareMap. Matching on "hardwareMap"
        // rather than "hardwareMap.get" also covers hardwareMap.dcMotor.get("name")
        // and hardwareMap.tryGet(...).
        val visitor = object : JavaRecursiveElementVisitor() {
            var found = false

            override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
                super.visitAssignmentExpression(expression)
                val lhs = expression.lExpression.text
                if (lhs == fieldName || lhs == "this.$fieldName") {
                    val rhs = expression.rExpression?.text ?: ""
                    if (rhs.contains("hardwareMap")) {
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

        if (extendsOpMode(psiClass)) return true

        // Also check if class has @TeleOp or @Autonomous annotation
        val annotations = psiClass.modifierList?.annotations ?: emptyArray()
        return annotations.any {
            it.qualifiedName?.contains("TeleOp") == true ||
                    it.qualifiedName?.contains("Autonomous") == true
        }
    }

    private fun extendsOpMode(psiClass: PsiClass): Boolean {
        // Check for LinearOpMode or OpMode in inheritance chain
        return psiClass.supers.any {
            it.qualifiedName?.contains("LinearOpMode") == true ||
                    it.qualifiedName?.contains("OpMode") == true
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
                val method = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiMethod::class.java)
                    ?: return@runWriteCommandAction
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
                val method = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiMethod::class.java)
                    ?: return@runWriteCommandAction
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

    // Quick Fix that registers the OpMode with the Driver Station
    private class AddOpModeAnnotationFix(
        private val qualifiedName: String,
        private val shortName: String
    ) : LocalQuickFix {
        override fun getName() = "Add @$shortName annotation"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    val clazz = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiClass::class.java)
                        ?: return@runWriteCommandAction
                    val modifierList = clazz.modifierList ?: return@runWriteCommandAction
                    val factory = JavaPsiFacade.getElementFactory(project)

                    val annotation = factory.createAnnotationFromText(
                        "@$qualifiedName(name = \"${clazz.name}\", group = \"$shortName\")",
                        clazz
                    )
                    val added = modifierList.addBefore(annotation, modifierList.firstChild)

                    // Turns the fully qualified name into a short one and adds the import.
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(added)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Quick Fix that swaps Thread.sleep(...) for the OpMode's interrupt aware sleep(...)
    private class ReplaceWithOpModeSleepFix : LocalQuickFix {
        override fun getName() = "Replace with the OpMode's sleep()"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    val call = descriptor.psiElement as? PsiMethodCallExpression
                        ?: return@runWriteCommandAction
                    val argument = call.argumentList.expressions.singleOrNull()
                        ?: return@runWriteCommandAction
                    val factory = JavaPsiFacade.getElementFactory(project)

                    call.replace(factory.createExpressionFromText("sleep(${argument.text})", call))
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Quick Fix that makes a loop stop when the match does
    private class AddOpModeIsActiveFix : LocalQuickFix {
        override fun getName() = "Add opModeIsActive() to the condition"
        override fun getFamilyName() = "FTC Quick Fixes"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    val condition = descriptor.psiElement as? PsiExpression
                        ?: return@runWriteCommandAction
                    val factory = JavaPsiFacade.getElementFactory(project)

                    // `while (true)` only ever needs the guard itself.
                    val replacement = if (condition.text.trim() == "true") {
                        "opModeIsActive()"
                    } else {
                        "opModeIsActive() && (${condition.text})"
                    }

                    condition.replace(factory.createExpressionFromText(replacement, condition))
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
                    val field = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiField::class.java)
                        ?: return@runWriteCommandAction
                    val containingClass = field.containingClass ?: return@runWriteCommandAction

                    // LinearOpMode puts setup in runOpMode(); the iterative OpMode uses init().
                    val method = containingClass.findMethodsByName("runOpMode", false).firstOrNull()
                        ?: containingClass.findMethodsByName("init", false).firstOrNull()
                        ?: return@runWriteCommandAction

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

    private companion object {
        const val TELEOP_ANNOTATION = "com.qualcomm.robotcore.eventloop.opmode.TeleOp"
        const val AUTONOMOUS_ANNOTATION = "com.qualcomm.robotcore.eventloop.opmode.Autonomous"

        val STOP_AWARE_CALLS = listOf("opModeIsActive", "isStopRequested", "opModeInInit")
    }
}
