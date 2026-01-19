package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

class SmartHardwareMapAction : AnAction("Generate HardwareMap") {

    private val supportedTypes = setOf(
        "DcMotor",
        "DcMotorEx",
        "Servo",
        "CRServo",
        "IMU",
        "BNO055IMU",
        "ColorSensor",
        "DistanceSensor"
    )

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val text = document.text

        // 1. Find field declarations
        val fieldRegex = Regex("""(\w+)\s+(\w+);""")
        val fields = fieldRegex.findAll(text)
            .map { it.groupValues[1] to it.groupValues[2] }
            .filter { it.first in supportedTypes }
            .toList()

        if (fields.isEmpty()) return

        // 2. Remove already initialized fields
        val missing = fields.filterNot { (_, name) ->
            text.contains("$name = hardwareMap.get")
        }

        if (missing.isEmpty()) return

        // 3. Generate code
        val generated = buildString {
            append("\n// Auto-generated HardwareMap\n")
            for ((type, name) in missing) {
                append("$name = hardwareMap.get($type.class, \"$name\");\n")
            }
        }

        // 4. Find insertion point
        val insertOffset =
            text.indexOf("init()").takeIf { it != -1 }
                ?: text.indexOf("runOpMode()").takeIf { it != -1 }
                ?: editor.caretModel.offset

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(insertOffset, generated)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
    }
}
