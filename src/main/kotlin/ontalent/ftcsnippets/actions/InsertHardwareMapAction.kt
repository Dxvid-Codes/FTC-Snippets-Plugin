//InsertHardwareMap.kt

package ontalent.ftcsnippets

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class InsertHardwareMapAction : AnAction("Insert HardwareMap") {
    override fun actionPerformed(e: AnActionEvent) {
        val varName = Messages.showInputDialog("Enter variable name:", "HardwareMap Generator", null) ?: return
        val type = Messages.showEditableChooseDialog(
            "Select hardware type:",
            "HardwareMap Generator",
            null,
            arrayOf("DcMotor", "Servo", "CRServo", "BNO055IMU"),
            "DcMotor",
            null
        ) ?: return
        val configName = Messages.showInputDialog(
            "Enter config name (leave blank = same as variable):",
            "HardwareMap Generator",
            null
        ) ?: varName

        val code = """
            $type $varName;
            $varName = hardwareMap.get($type.class, "$configName");
        """.trimIndent()

        val editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR) ?: return

        // Use WriteCommandAction to allow document modification safely
        WriteCommandAction.runWriteCommandAction(editor.project) {
            val caret = editor.caretModel.offset
            editor.document.insertString(caret, "\n$code\n")
        }
    }
}
