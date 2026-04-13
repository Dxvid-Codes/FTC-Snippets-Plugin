package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import kotlin.math.PI

class RoadrunnerToPedroAction : AnAction("Convert RoadRunner → Pedro") {

    private val fieldHalf = 72.0

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document

        val selection = editor.selectionModel
        val start = if (selection.hasSelection()) selection.selectionStart else 0
        val end = if (selection.hasSelection()) selection.selectionEnd else document.textLength

        val originalText = document.getText(com.intellij.openapi.util.TextRange(start, end))
        val convertedText = convertRoadRunnerCode(originalText)

        if (originalText == convertedText) {
            Messages.showInfoMessage(project, "No RoadRunner-style coordinates found.", "RoadRunner → Pedro")
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, convertedText)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    private fun convertRoadRunnerCode(text: String): String {
        // Only converts Pose2d — heading third arg goes from radians back to degrees
        // Scoped tightly to new Pose2d(...) to avoid touching unrelated numbers
        val poseRegex = Regex("""new\s+Pose2d\s*\(\s*([-0-9.]+)\s*,\s*([-0-9.]+)\s*,\s*([-0-9.]+)\s*\)""")

        return text.replace(poseRegex) { match ->
            val x = match.groupValues[1].toDouble() + fieldHalf
            val y = match.groupValues[2].toDouble() + fieldHalf
            val headingRad = match.groupValues[3].toDouble()
            val headingDeg = headingRad * 180.0 / PI
            "new Pose(${format(x)}, ${format(y)}, ${format(headingDeg)})"
        }
    }

    private fun format(value: Double): String = String.format("%.4f", value)
}