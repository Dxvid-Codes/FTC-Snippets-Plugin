/** InsertTeleOpImportsAction.kt */

package ontalent.ftcsnippets.actions

class InsertTeleOpImportsAction : AbstractInsertSkeletonAction(
    text = "TeleOp Skeleton",
    description = "Insert TeleOp template code"
) {
    override val fallbackClassName = "MyTeleOp"

    override fun imports(kotlin: Boolean): List<String> = listOf(
        "com.qualcomm.robotcore.eventloop.opmode.LinearOpMode",
        "com.qualcomm.robotcore.eventloop.opmode.TeleOp"
    )

    override fun skeleton(className: String, kotlin: Boolean): String = if (kotlin) {
        """
        @TeleOp(name = "$className", group = "TeleOp")
        class $className : LinearOpMode() {

            override fun runOpMode() {
                // TODO: Essentially your main method

                waitForStart()

                while (opModeIsActive()) {
                    // Your loop
                }
            }
        }
        """.trimIndent()
    } else {
        """
        @TeleOp(name = "$className", group = "TeleOp")
        public class $className extends LinearOpMode {

            @Override
            public void runOpMode() throws InterruptedException {
                // TODO: Essentially your main method

                waitForStart();

                while (opModeIsActive()) {
                    // Your loop
                }
            }
        }
        """.trimIndent()
    }
}
