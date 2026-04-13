package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager

class InsertTeleOpImportsAction : AnAction(
    "TeleOp Skeleton",
    "Insert TeleOp template code",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val document = editor.document

        val fileName = psiFile.virtualFile?.nameWithoutExtension ?: "MyTeleOp"

        val imports = """
            import com.qualcomm.robotcore.eventloop.opmode.OpMode;
            import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
            import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
        """.trimIndent()

        val skeleton = """
            
            @TeleOp(name = "$fileName", group = "TeleOp")
            public class $fileName extends LinearOpMode { 
                
                @Override
                public void runOpMode() throws InterruptedException {
                    // TODO: Essentially your main method
                    
                    waitForStart();
                    
                    while(opModeIsActive()) {
                        // Your loop
                    }
                }
            }
        """.trimIndent()

        WriteCommandAction.runWriteCommandAction(project) {
            val fileText = document.text

            // Add package if missing
            val hasPackage = Regex("""^\s*package\s+.+;""", RegexOption.MULTILINE)
                .containsMatchIn(fileText)

            if (!hasPackage) {
                val correctPackage = getCorrectPackageFromPath(psiFile)
                document.insertString(0, "package $correctPackage;\n\n")
            }

            // Insert imports after existing imports
            val importOffset = findImportInsertPosition(document)
            document.insertString(importOffset, "$imports\n")

            // Insert skeleton at end of file
            val skeletonOffset = document.textLength
            document.insertString(skeletonOffset, "$skeleton\n")

            // Reformat
            try {
                CodeStyleManager.getInstance(project)
                    .reformatText(psiFile, 0, document.textLength)
            } catch (_: Exception) {
                // Non-fatal
            }
        }
    }

    private fun getCorrectPackageFromPath(
        psiFile: PsiFile,
        basePackage: String = "org.firstinspires.ftc.teamcode"
    ): String {
        val virtualFile = psiFile.virtualFile ?: return basePackage
        val filePath = virtualFile.path.replace('\\', '/')

        val patterns = listOf("/teamcode/", "/TeamCode/")
        var teamcodeIndex = -1
        var foundPattern = ""

        for (pattern in patterns) {
            val index = filePath.indexOf(pattern)
            if (index != -1) {
                teamcodeIndex = index
                foundPattern = pattern
                break
            }
        }

        if (teamcodeIndex == -1) return basePackage

        // Everything after /teamcode/ up to (not including) the filename
        val afterTeamcode = filePath.substring(teamcodeIndex + foundPattern.length)
        val lastSlash = afterTeamcode.lastIndexOf('/')

        return if (lastSlash == -1) {
            // File is directly inside teamcode/
            basePackage
        } else {
            // File is in a subfolder e.g. teamcode/autonomous/MyAuto.java
            val subPath = afterTeamcode.substring(0, lastSlash).trim('/')
            "$basePackage.${subPath.replace('/', '.')}"
        }
    }

    private fun findImportInsertPosition(document: Document): Int {
        val text = document.text
        var offset = 0

        val packageMatch = Regex("""^\s*package\s+.+;""", RegexOption.MULTILINE).find(text)
        if (packageMatch != null) {
            offset = packageMatch.range.last + 1
            if (offset < text.length && text[offset] == '\n') offset++
        }

        // Find LAST import
        val importMatches = Regex("""^\s*import\s+.+;""", RegexOption.MULTILINE).findAll(text)
        val lastImport = importMatches.lastOrNull()

        if (lastImport != null) {
            offset = lastImport.range.last + 1
            if (offset < text.length && text[offset] == '\n') offset++
        }

        return offset.coerceAtMost(document.textLength)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
        e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
    }
}

/*
private fun getCorrectPackageFromPath(
    psiFile: PsiFile,
    basePackage: String = "org.firstinspires.ftc.teamcode"
): String {
    val virtualFile = psiFile.virtualFile ?: return basePackage
    val filePath = virtualFile.path.replace('\\', '/')

    val patterns = listOf("/teamcode/", "/TeamCode/")
    var teamcodeIndex = -1
    var foundPattern = ""

    for (pattern in patterns) {
        val index = filePath.indexOf(pattern)
        if (index != -1) {
            teamcodeIndex = index
            foundPattern = pattern
            break
        }
    }

    if (teamcodeIndex == -1) return basePackage

    // Everything after /teamcode/ up to (not including) the filename
    val afterTeamcode = filePath.substring(teamcodeIndex + foundPattern.length)
    val lastSlash = afterTeamcode.lastIndexOf('/')

    return if (lastSlash == -1) {
        // File is directly inside teamcode/
        basePackage
    } else {
        // File is in a subfolder e.g. teamcode/autonomous/MyAuto.java
        val subPath = afterTeamcode.substring(0, lastSlash).trim('/')
        "$basePackage.${subPath.replace('/', '.')}"
    }
}

override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
}
 */