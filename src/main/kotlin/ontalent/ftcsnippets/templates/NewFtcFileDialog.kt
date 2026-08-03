/** NewFtcFileDialog.kt */

package ontalent.ftcsnippets.templates

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import ontalent.ftcsnippets.settings.FtcFramework
import ontalent.ftcsnippets.settings.FtcLanguage
import ontalent.ftcsnippets.settings.FtcSnippetsSettings
import ontalent.ftcsnippets.settings.SubsystemPattern
import javax.swing.JComponent

/**
 * Asks for the class name and the flavor of the file about to be generated.
 * Every combo box starts on the default configured under `Tools -> FTC Snippets`.
 */
class NewFtcFileDialog(
    project: Project,
    private val kind: FtcTemplateKind,
    private val targetPackage: String
) : DialogWrapper(project) {

    private val nameField = JBTextField(kind.defaultClassName)
    private val languageCombo = ComboBox(FtcLanguage.values())
    private val frameworkCombo = ComboBox(FtcFramework.values())
    private val patternCombo = ComboBox(SubsystemPattern.values())

    val className: String get() = nameField.text.trim()
    val language: FtcLanguage get() = languageCombo.selectedItem as? FtcLanguage ?: FtcLanguage.JAVA
    val framework: FtcFramework get() = frameworkCombo.selectedItem as? FtcFramework ?: FtcFramework.SDK
    val pattern: SubsystemPattern
        get() = patternCombo.selectedItem as? SubsystemPattern ?: SubsystemPattern.SIMPLE

    init {
        val defaults = FtcSnippetsSettings.state()
        languageCombo.selectedItem = defaults.defaultLanguage
        frameworkCombo.selectedItem = defaults.defaultFramework
        patternCombo.selectedItem = defaults.defaultSubsystemPattern

        title = "New FTC ${kind.displayName}"
        setOKButtonText("Create")
        init()
    }

    override fun getPreferredFocusedComponent(): JComponent = nameField

    override fun createCenterPanel(): JComponent = panel {
        row("Name:") {
            cell(nameField).columns(30)
        }
        row("Language:") {
            cell(languageCombo)
        }
        row("Framework:") {
            cell(frameworkCombo)
        }
        if (kind.supportsSubsystemPattern) {
            row("Structure:") {
                cell(patternCombo)
            }.rowComment(
                "The structured pattern splits hardware access into read(), loop() and write()."
            )
        }
        row("Target package:") {
            label(if (targetPackage.isBlank()) "(default package)" else targetPackage)
        }
    }

    override fun doValidate(): ValidationInfo? {
        val name = className
        if (name.isEmpty()) {
            return ValidationInfo("Enter a class name.", nameField)
        }
        if (!name.first().isJavaIdentifierStart() || !name.all { it.isJavaIdentifierPart() }) {
            return ValidationInfo("'$name' is not a valid class name.", nameField)
        }
        return null
    }
}
