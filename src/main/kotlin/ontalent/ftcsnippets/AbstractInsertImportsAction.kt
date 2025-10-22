package ontalent.ftcsnippets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlin.math.min

/**
 * Base action for inserting import blocks and optional skeleton code into the current file.
 * Subclasses provide:
 *   - importsBlock(): multiline string of imports
 *   - classSkeleton(): optional code skeleton to insert after imports
 */
abstract class AbstractInsertImportsAction : AnAction() {

    /** Subclasses return the multiline import block they want inserted. */
    protected abstract fun importsBlock(): String

    /** Subclasses can override to provide a class skeleton (optional). */
    protected open fun classSkeleton(): String = ""

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val document: Document = editor.document

        val requiredPackage = "package org.firstinspires.ftc.teamcode;"

        val imports = importsBlock().trim()
        val skeleton = classSkeleton().trim()

        // Filter out imports already present
        val toAddImports = filterExistingImports(document.text, imports)
        if (toAddImports.isBlank() && skeleton.isBlank()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val fileText = document.text

            if(!fileText.contains(requiredPackage)) {
                document.insertString(0, "$requiredPackage\n\n")
            }

            if (toAddImports.isNotBlank()) {
                val insertPos = findInsertPosition(document)
                document.insertString(insertPos, "\n$toAddImports\n\n")
            }

            // Insert skeleton code after imports
            if (skeleton.isNotBlank()) {
                val offset = document.textLength
                document.insertString(offset, "\n$skeleton\n")
            }

            // Reformat the file safely
            try {
                CodeStyleManager.getInstance(project).reformatText(psiFile, 0, document.textLength)
            } catch (_: Exception) {
                // ignore formatting exceptions
            }
        }
    }

    /** Filter out lines that already exist in the file. */
    private fun filterExistingImports(fileText: String, importsBlock: String): String {
        val sb = StringBuilder()
        for (line in importsBlock.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (!fileText.contains(trimmed)) sb.append(trimmed).append("\n")
        }
        return sb.toString().trimEnd()
    }

    /**
     * Determine insert position:
     *   - After 'package' declaration if present
     *   - Before first 'import' if present
     *   - Otherwise start of document
     */
    private fun findInsertPosition(document: Document): Int {
        val text = document.text
        val lines = text.split("\n")
        var offset = 0
        for (line in lines) {
            if (line.startsWith("package ")) {
                offset += line.length + 1
                continue
            }
            if (line.startsWith("import ")) {
                // insert before first import
                break
            }
            offset += line.length + 1
        }
        return offset.coerceAtMost(document.textLength)
    }
}
