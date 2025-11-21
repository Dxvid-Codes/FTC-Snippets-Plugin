/** InsertAllImportsAction.kt */

package ontalent.ftcsnippets.actions

class InsertAllImportsAction : AbstractInsertImportsAction() {
    override fun importsBlock(): String = """
        import com.qualcomm.robotcore.hardware.DcMotor;
        import com.qualcomm.robotcore.hardware.DcMotorSimple;
        import com.qualcomm.robotcore.hardware.Servo;
        import com.qualcomm.robotcore.hardware.CRServo;
        import com.qualcomm.robotcore.hardware.HardwareMap;
        import com.qualcomm.robotcore.hardware.DistanceSensor;
        import com.qualcomm.robotcore.hardware.ColorSensor;
        import com.qualcomm.robotcore.hardware.TouchSensor;
        import com.qualcomm.robotcore.hardware.VoltageSensor;
        import com.qualcomm.robotcore.hardware.IMU;
        import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
        import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
        import org.firstinspires.ftc.robotcore.external.ClassFactory;
        import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
        import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
        import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
        import com.qualcomm.robotcore.util.ElapsedTime;
    """.trimIndent()
}
