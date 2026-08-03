/** FtcTemplateKind.kt */

package ontalent.ftcsnippets.templates

/** The kinds of complete class files the `New -> FTC` group can generate. */
enum class FtcTemplateKind(
    val displayName: String,
    val description: String,
    val defaultClassName: String,
    /** Only subsystems expose the read()/loop()/write() choice. */
    val supportsSubsystemPattern: Boolean = false
) {
    AUTONOMOUS(
        displayName = "Autonomous OpMode",
        description = "A @Autonomous OpMode ready for a match routine",
        defaultClassName = "MyAutonomous"
    ),
    TELEOP(
        displayName = "TeleOp OpMode",
        description = "A @TeleOp OpMode with a driver control loop",
        defaultClassName = "MyTeleOp"
    ),
    SUBSYSTEM(
        displayName = "Subsystem",
        description = "A hardware subsystem wrapper",
        defaultClassName = "MySubsystem",
        supportsSubsystemPattern = true
    ),
    COMMAND(
        displayName = "Command",
        description = "A command that acts on a subsystem",
        defaultClassName = "MyCommand"
    ),
    ROBOT_CONTAINER(
        displayName = "Robot Container",
        description = "A container that owns every subsystem and its bindings",
        defaultClassName = "RobotContainer"
    );

    override fun toString(): String = displayName
}
