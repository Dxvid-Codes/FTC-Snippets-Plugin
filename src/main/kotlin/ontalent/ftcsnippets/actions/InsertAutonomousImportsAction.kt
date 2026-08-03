/** InsertAutonomousImportsAction.kt */

package ontalent.ftcsnippets.actions

class InsertAutonomousImportsAction : AbstractInsertSkeletonAction(
    text = "Autonomous Skeleton",
    description = "Insert Autonomous template code"
) {
    override val fallbackClassName = "MyAutonomous"

    override fun imports(kotlin: Boolean): List<String> = listOf(
        "com.qualcomm.robotcore.eventloop.opmode.Autonomous",
        "com.qualcomm.robotcore.eventloop.opmode.LinearOpMode"
    )

    override fun skeleton(className: String, kotlin: Boolean): String = if (kotlin) {
        """
        @Autonomous(name = "$className", group = "Autonomous")
        class $className : LinearOpMode() {

            override fun runOpMode() {
                // TODO: Set up autonomous steps

                waitForStart()

                if (opModeIsActive()) {
                    // Your autonomous sequence
                }
            }
        }
        """.trimIndent()
    } else {
        """
        @Autonomous(name = "$className", group = "Autonomous")
        public class $className extends LinearOpMode {

            @Override
            public void runOpMode() throws InterruptedException {
                // TODO: Set up autonomous steps

                waitForStart();

                if (opModeIsActive()) {
                    // Your autonomous sequence
                }
            }
        }
        """.trimIndent()
    }
}
