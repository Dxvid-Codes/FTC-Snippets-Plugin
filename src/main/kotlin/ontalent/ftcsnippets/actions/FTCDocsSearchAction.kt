// FTCDcosSearchAction.kt

package ontalent.ftcsnippets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.ide.BrowserUtil

class FTCDocsSearchAction : AnAction("FTC Docs Search") {
    override fun actionPerformed(e: AnActionEvent) {
        val query = Messages.showInputDialog(
            e.project,
            "Enter FTC topic to search:",
            "FTC Docs Search",
            Messages.getQuestionIcon()
        )

        if (!query.isNullOrBlank()) {
            val encodedQuery = query.trim().replace(" ", "+")
            val url = "https://ftc-docs.firstinspires.org/en/latest/search.html?q=$encodedQuery"
            try {
                BrowserUtil.browse(url)
            } catch (ex: Exception) {
                BrowserUtil.browse("https://ftc-docs.firstinspires.org/en/latest/")
            }
        }
    }
}
