package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.codeStyle.CodeStyleManager

abstract class AbstractInsertImportsAction : AnAction() {

    protected abstract fun importsBlock(): String
    protected open fun classSkeleton(): String = ""

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val document: Document = editor.document

        val requiredPackage = "package org.firstinspires.ftc.teamcode;"
        val imports = importsBlock().trim()
        val skeleton = classSkeleton().trim()
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

            if (skeleton.isNotBlank()) {
                val offset = document.textLength
                document.insertString(offset, "\n$skeleton\n")
            }

            try {
                CodeStyleManager.getInstance(project).reformatText(psiFile, 0, document.textLength)
            } catch (_: Exception) {
                // ignore formatting exceptions
            }
        }
    }

    override fun update(e: AnActionEvent) {
        // ALWAYS ENABLED - let actionPerformed handle the logic
        e.presentation.isEnabled = true
        e.presentation.isVisible = true
    }

    private fun filterExistingImports(fileText: String, importsBlock: String): String {
        val sb = StringBuilder()
        for (line in importsBlock.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue
            if (!fileText.contains(trimmed)) sb.append(trimmed).append("\n")
        }
        return sb.toString().trimEnd()
    }

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
                break
            }
            offset += line.length + 1
        }
        return offset.coerceAtMost(document.textLength)
    }
}