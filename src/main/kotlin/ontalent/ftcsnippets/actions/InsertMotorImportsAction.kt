/** InserMotorImportsAction.kt */

package ontalent.ftcsnippets

class InsertMotorImportsAction : AbstractInsertImportsAction() {
    override fun importsBlock(): String = """
        import com.qualcomm.robotcore.hardware.DcMotor;
        import com.qualcomm.robotcore.hardware.DcMotorSimple;
        import com.qualcomm.robotcore.hardware.HardwareMap;
        import com.qualcomm.robotcore.util.ElapsedTime;
    """.trimIndent()
}
