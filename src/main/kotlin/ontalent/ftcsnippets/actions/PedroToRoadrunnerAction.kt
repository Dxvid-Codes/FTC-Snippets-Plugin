package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import kotlin.math.PI

class PedroToRoadrunnerAction : AnAction("Convert Pedro → RoadRunner") {

    private val FIELD_HALF = 72.0

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document

        val selectionModel = editor.selectionModel
        val hasSelection = selectionModel.hasSelection()
        val start = if (hasSelection) selectionModel.selectionStart else 0
        val end = if (hasSelection) selectionModel.selectionEnd else document.textLength

        val originalText = document.getText(com.intellij.openapi.util.TextRange(start, end))
        val convertedText = convertPedroCode(originalText)

        if (originalText == convertedText) {
            Messages.showInfoMessage(project, "No Pedro-style coordinates found.", "Pedro → RoadRunner")
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, convertedText)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    private fun convertPedroCode(text: String): String {
        // Handles negative coords with [-0-9.]+ and converts heading from degrees to radians
        val poseRegex = Regex("""new\s+Pose\s*\(\s*([-0-9.]+)\s*,\s*([-0-9.]+)\s*,\s*([-0-9.]+)\s*\)""")

        return text.replace(poseRegex) { match ->
            val x = match.groupValues[1].toDouble() - FIELD_HALF
            val y = match.groupValues[2].toDouble() - FIELD_HALF
            val headingDeg = match.groupValues[3].toDouble()
            val headingRad = headingDeg * PI / 180.0
            "new Pose2d(${format(x)}, ${format(y)}, ${format(headingRad)})"
        }
    }

    private fun format(value: Double): String = String.format("%.4f", value)
}