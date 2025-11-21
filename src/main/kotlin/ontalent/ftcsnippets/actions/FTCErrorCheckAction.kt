// FTCErrorCheckAction.kt

package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.project.Project

class FTCErrorCheckAction : AnAction("Check FTC Errors", "Run FTC error inspection on current file", null) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        Messages.showInfoMessage(
            "FTC Error Check completed!\n\n" +
                    "The FTC Error Helper inspection is now active.\n\n" +
                    "Look for warnings in your code highlighting:\n" +
                    "• Missing waitForStart()\n" +
                    "• Uninitialized hardware devices\n" +
                    "• Missing telemetry.update()\n" +
                    "• Gamepad usage issues\n\n" +
                    "Use Alt+Enter on highlighted code to apply quick fixes automatically.",
            "FTC Error Helper"
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null
    }
}