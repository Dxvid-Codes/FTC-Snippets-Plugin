package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages

class SmartHardwareMapAction : AnAction("Generate HardwareMap") {

    private val supportedTypes = setOf(
        "DcMotor", "DcMotorEx", "Servo", "CRServo",
        "IMU", "BNO055IMU", "ColorSensor", "DistanceSensor"
    )

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val text = document.text

        // Match field declarations like: DcMotor leftFront;
        val fieldRegex = Regex(
            """^\s*(${supportedTypes.joinToString("|")})\s+(\w+)\s*;""",
            RegexOption.MULTILINE
        )
        val fields = fieldRegex.findAll(text)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()

        if (fields.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No supported hardware field declarations found.\n\nDeclare fields like:\n  DcMotor leftFront;",
                "Generate HardwareMap"
            )
            return
        }

        val missing = fields.filterNot { (_, name) ->
            text.contains("$name = hardwareMap.get")
        }

        if (missing.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "All hardware fields are already initialized.",
                "Generate HardwareMap"
            )
            return
        }

        val generated = buildString {
            append("\n        // Auto-generated HardwareMap\n")
            for ((type, name) in missing) {
                append("        $name = hardwareMap.get($type.class, \"$name\");\n")
            }
        }

        // Match the opening brace of init() or runOpMode() regardless of modifiers/throws
        val insertOffset = run {
            val methodRegex = Regex("""(void\s+init|void\s+runOpMode)\s*\([^)]*\)[^{]*\{""")
            val match = methodRegex.find(text)
            if (match != null) {
                match.range.last + 1
            } else {
                // Fall back to caret position and warn the user
                Messages.showWarningDialog(
                    project,
                    "Could not find init() or runOpMode() — inserting at cursor position.",
                    "Generate HardwareMap"
                )
                editor.caretModel.offset
            }
        }

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(insertOffset, generated)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
        e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
    }
}