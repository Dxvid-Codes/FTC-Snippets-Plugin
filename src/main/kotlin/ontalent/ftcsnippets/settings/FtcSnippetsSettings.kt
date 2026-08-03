/** FtcSnippetsSettings.kt */

package ontalent.ftcsnippets.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/** Language a generated FTC file is written in. */
enum class FtcLanguage(private val displayName: String, val extension: String) {
    JAVA("Java", "java"),
    KOTLIN("Kotlin", "kt");

    override fun toString(): String = displayName
}

/** Flavor of the generated code: plain FTC SDK, or SolversLib command-based. */
enum class FtcFramework(private val displayName: String) {
    SDK("FTC SDK (plain)"),
    SOLVERSLIB("SolversLib (command-based)");

    override fun toString(): String = displayName
}

/** Structure used for generated subsystems. */
enum class SubsystemPattern(private val displayName: String) {
    SIMPLE("Simple"),
    READ_LOOP_WRITE("read() / loop() / write()");

    override fun toString(): String = displayName
}

/**
 * Application-level defaults for the file generators. Persisted to `ftcSnippets.xml`
 * in the IDE config directory.
 */
@Service(Service.Level.APP)
@State(name = "FtcSnippetsSettings", storages = [Storage("ftcSnippets.xml")])
class FtcSnippetsSettings : PersistentStateComponent<FtcSnippetsSettings.State> {

    class State {
        @JvmField var defaultLanguage: FtcLanguage = FtcLanguage.JAVA
        @JvmField var defaultFramework: FtcFramework = FtcFramework.SDK
        @JvmField var defaultSubsystemPattern: SubsystemPattern = SubsystemPattern.SIMPLE
        @JvmField var basePackage: String = DEFAULT_BASE_PACKAGE
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        const val DEFAULT_BASE_PACKAGE = "org.firstinspires.ftc.teamcode"

        fun getInstance(): FtcSnippetsSettings =
            ApplicationManager.getApplication().getService(FtcSnippetsSettings::class.java)

        /** Convenience accessor so callers can read defaults without unwrapping the service. */
        fun state(): State = getInstance().state
    }
}
