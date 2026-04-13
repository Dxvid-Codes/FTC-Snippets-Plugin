/** InsertIMUImportsAction.kt */

package ontalent.ftcsnippets.actions

class InsertIMUImportsAction : AbstractInsertImportsAction(
    text = "Insert IMU Imports",
    description = "Inserts all necessary IMU Imports"
) {
    override fun getInsertText(): String = """
        import com.qualcomm.robotcore.hardware.IMU;
        import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
        import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
        import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
        import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
        import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
    """.trimIndent()
}
