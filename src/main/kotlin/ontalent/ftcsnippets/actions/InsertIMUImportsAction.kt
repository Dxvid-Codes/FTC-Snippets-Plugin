/** InsertIMUImportsAction.kt */

package ontalent.ftcsnippets

class InsertIMUImportsAction : AbstractInsertImportsAction() {
    override fun importsBlock(): String = """
        import com.qualcomm.robotcore.hardware.IMU;
        import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
        import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
        import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
    """.trimIndent()
}
