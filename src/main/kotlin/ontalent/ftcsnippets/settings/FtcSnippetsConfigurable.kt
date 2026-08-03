/** FtcSnippetsConfigurable.kt */

package ontalent.ftcsnippets.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*

/**
 * Settings page shown under `Tools -> FTC Snippets`. These values pre-fill the
 * `New -> FTC` generator dialogs.
 */
class FtcSnippetsConfigurable : BoundConfigurable("FTC Snippets") {

    private val state = FtcSnippetsSettings.state()

    override fun createPanel(): DialogPanel = panel {
        group("File Generation Defaults") {
            row("Default language:") {
                comboBox(FtcLanguage.values().toList())
                    .bindItem(state::defaultLanguage.toNullableProperty())
            }.rowComment("Language pre-selected in the <b>New -> FTC</b> dialogs.")

            row("Default framework:") {
                comboBox(FtcFramework.values().toList())
                    .bindItem(state::defaultFramework.toNullableProperty())
            }.rowComment("SolversLib generates command-based classes; the SDK flavor stays dependency free.")

            row("Default subsystem pattern:") {
                comboBox(SubsystemPattern.values().toList())
                    .bindItem(state::defaultSubsystemPattern.toNullableProperty())
            }.rowComment("The structured pattern splits hardware access into read(), loop() and write().")
        }

        group("Package") {
            row("Base package:") {
                textField()
                    .bindText(state::basePackage)
                    .columns(36)
            }.rowComment(
                "Fallback package for generated files that are created outside a <code>teamcode</code> folder."
            )
        }
    }
}
