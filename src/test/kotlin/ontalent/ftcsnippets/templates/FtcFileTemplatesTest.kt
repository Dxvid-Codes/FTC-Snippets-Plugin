package ontalent.ftcsnippets.templates

import ontalent.ftcsnippets.settings.FtcFramework
import ontalent.ftcsnippets.settings.FtcLanguage
import ontalent.ftcsnippets.settings.SubsystemPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The templates build their text by interpolating optional blocks into trimIndent()
 * strings, which is easy to get subtly wrong, so every combination is rendered and
 * sanity checked here.
 */
class FtcFileTemplatesTest {

    private fun render(
        kind: FtcTemplateKind,
        language: FtcLanguage = FtcLanguage.JAVA,
        framework: FtcFramework = FtcFramework.SDK,
        pattern: SubsystemPattern = SubsystemPattern.SIMPLE
    ) = FtcFileTemplates.render(
        kind = kind,
        className = "Widget",
        packageName = "org.firstinspires.ftc.teamcode.sub",
        language = language,
        framework = framework,
        pattern = pattern
    )

    private fun allCombinations(): List<Triple<FtcTemplateKind, String, String>> =
        FtcTemplateKind.values().flatMap { kind ->
            FtcLanguage.values().flatMap { language ->
                FtcFramework.values().flatMap { framework ->
                    SubsystemPattern.values().map { pattern ->
                        Triple(
                            kind,
                            "$kind/$language/$framework/$pattern",
                            render(kind, language, framework, pattern)
                        )
                    }
                }
            }
        }

    @Test
    fun `every combination renders balanced braces`() {
        allCombinations().forEach { (_, label, text) ->
            val open = text.count { it == '{' }
            val close = text.count { it == '}' }
            assertEquals("unbalanced braces in $label", open, close)
        }
    }

    @Test
    fun `every combination starts with the package statement and names the class`() {
        allCombinations().forEach { (_, label, text) ->
            assertTrue(
                "missing package in $label",
                text.startsWith("package org.firstinspires.ftc.teamcode.sub")
            )
            assertTrue("missing class name in $label", text.contains("Widget"))
        }
    }

    @Test
    fun `java is terminated and kotlin is not`() {
        val java = render(FtcTemplateKind.TELEOP, FtcLanguage.JAVA)
        val kotlin = render(FtcTemplateKind.TELEOP, FtcLanguage.KOTLIN)

        assertTrue(java.contains("package org.firstinspires.ftc.teamcode.sub;"))
        assertTrue(java.lineSequence().any { it == "import com.qualcomm.robotcore.eventloop.opmode.TeleOp;" })

        assertFalse(kotlin.lineSequence().any { it.trimEnd().endsWith(";") })
        assertTrue(kotlin.lineSequence().any { it == "import com.qualcomm.robotcore.eventloop.opmode.TeleOp" })
    }

    @Test
    fun `trimIndent strips the template literal's own indentation`() {
        // The templates interpolate optional blocks into trimIndent() strings. If the
        // common indent were computed wrongly the whole class would come out shifted,
        // so pin the two lines that must sit flush left.
        allCombinations().forEach { (_, label, text) ->
            val declaration = text.lineSequence().first { it.contains("class Widget") }
            assertEquals("class declaration is indented in $label", declaration.trimStart(), declaration)

            val lastLine = text.trimEnd().lines().last()
            assertEquals("class is not closed flush left in $label", "}", lastLine)
        }
    }

    @Test
    fun `structured subsystems expose read loop and write at member indentation`() {
        listOf(FtcLanguage.JAVA, FtcLanguage.KOTLIN).forEach { language ->
            FtcFramework.values().forEach { framework ->
                val text = render(
                    FtcTemplateKind.SUBSYSTEM, language, framework, SubsystemPattern.READ_LOOP_WRITE
                )
                val label = "$language/$framework"

                listOf("read()", "loop()", "write()").forEach {
                    assertTrue("structured subsystem $label is missing $it", text.contains(it))
                }

                // SolversLib subsystems drive the three phases from periodic(); that block is
                // interpolated in, so check it lands at the same indent as the other members.
                if (framework == FtcFramework.SOLVERSLIB) {
                    val periodic = text.lineSequence().first { it.contains("periodic()") }
                    assertEquals("periodic() misindented in $label", "    ", periodic.takeWhile { it == ' ' })
                }
            }
        }
    }

    @Test
    fun `simple subsystems omit the structured methods`() {
        val text = render(
            FtcTemplateKind.SUBSYSTEM, FtcLanguage.JAVA, FtcFramework.SDK, SubsystemPattern.SIMPLE
        )
        assertFalse(text.contains("fun read()"))
        assertFalse(text.contains("public void read()"))
    }

    @Test
    fun `solverslib templates import solverslib and sdk templates do not`() {
        FtcTemplateKind.values().forEach { kind ->
            FtcLanguage.values().forEach { language ->
                val sdk = render(kind, language, FtcFramework.SDK)
                assertFalse(
                    "plain SDK $kind/$language leaks a SolversLib import",
                    sdk.contains("com.seattlesolvers")
                )
            }
        }

        // Commands under the plain SDK are deliberately standalone, so only the
        // framework-backed kinds are required to pull SolversLib in.
        listOf(
            FtcTemplateKind.AUTONOMOUS,
            FtcTemplateKind.TELEOP,
            FtcTemplateKind.SUBSYSTEM,
            FtcTemplateKind.COMMAND,
            FtcTemplateKind.ROBOT_CONTAINER
        ).forEach { kind ->
            FtcLanguage.values().forEach { language ->
                val solvers = render(kind, language, FtcFramework.SOLVERSLIB)
                assertTrue(
                    "$kind/$language should import SolversLib",
                    solvers.contains("import com.seattlesolvers.solverslib")
                )
            }
        }
    }

    @Test
    fun `opmodes carry their registration annotation`() {
        FtcLanguage.values().forEach { language ->
            FtcFramework.values().forEach { framework ->
                assertTrue(render(FtcTemplateKind.AUTONOMOUS, language, framework).contains("@Autonomous(name = \"Widget\""))
                assertTrue(render(FtcTemplateKind.TELEOP, language, framework).contains("@TeleOp(name = \"Widget\""))
            }
        }
    }
}
