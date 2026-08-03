/** FtcPackages.kt */

package ontalent.ftcsnippets.templates

import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory
import ontalent.ftcsnippets.settings.FtcSnippetsSettings

/** Works out the `package` statement a generated file should carry. */
object FtcPackages {

    /**
     * Prefers the package the IDE derives from the module's source roots, because that
     * is the only answer guaranteed to compile. Falls back to the same `teamcode` path
     * heuristic the import actions use, and finally to the configured base package.
     */
    fun resolve(directory: PsiDirectory): String {
        val fromSourceRoot = try {
            JavaDirectoryService.getInstance().getPackage(directory)?.qualifiedName
        } catch (_: Exception) {
            null
        }
        if (!fromSourceRoot.isNullOrBlank()) return fromSourceRoot

        fromDirectoryPath(directory.virtualFile.path)?.let { return it }

        // A directory that really is the source root maps to the default package.
        return fromSourceRoot ?: basePackage()
    }

    /**
     * Maps `.../teamcode/autonomous` to `<base>.autonomous`. Returns null when the
     * directory is not inside a `teamcode` folder, so the caller can pick a fallback.
     */
    fun fromDirectoryPath(rawPath: String): String? {
        val path = rawPath.replace('\\', '/').trimEnd('/')
        val marker = "/teamcode"

        val index = path.indexOf(marker)
        if (index == -1) return null

        val after = path.substring(index + marker.length)
        // Guard against a folder such as /teamcodeUtils that merely starts the same way.
        if (after.isNotEmpty() && !after.startsWith("/")) return null

        val suffix = after.trim('/')
        return if (suffix.isEmpty()) basePackage() else "${basePackage()}.${suffix.replace('/', '.')}"
    }

    private fun basePackage(): String {
        val configured = FtcSnippetsSettings.state().basePackage.trim()
        return configured.ifEmpty { FtcSnippetsSettings.DEFAULT_BASE_PACKAGE }
    }
}
