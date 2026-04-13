package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

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
        // Anchored to start of line to avoid matching return statements etc.
        val fieldRegex = Regex("""^\s*(${supportedTypes.joinToString("|")})\s+(\w+)\s*;""", RegexOption.MULTILINE)
        val fields = fieldRegex.findAll(text)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()

        if (fields.isEmpty()) return

        val missing = fields.filterNot { (_, name) ->
            text.contains("$name = hardwareMap.get")
        }

        if (missing.isEmpty()) return

        val generated = buildString {
            append("\n        // Auto-generated HardwareMap\n")
            for ((type, name) in missing) {
                append("        $name = hardwareMap.get($type.class, \"$name\");\n")
            }
        }

        // Insert inside the opening brace of init() or runOpMode()
        val insertOffset = run {
            val methodIdx = text.indexOf("void init()")
                .takeIf { it != -1 }
                ?: text.indexOf("void runOpMode()")
                    .takeIf { it != -1 }
                ?: return@run editor.caretModel.offset
            val braceIdx = text.indexOf('{', methodIdx)
            if (braceIdx != -1) braceIdx + 1 else editor.caretModel.offset
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