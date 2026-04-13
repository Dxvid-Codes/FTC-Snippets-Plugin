/** InserMotorImportsAction.kt */

package ontalent.ftcsnippets.actions

class InsertMotorImportsAction : AbstractInsertImportsAction(
    text = "Insert Motor Imports",
    description = "Inserts motor imports to configure your robot and make it drive"
) {
    override fun getInsertText(): String = """
        import com.qualcomm.robotcore.hardware.DcMotor;
        import com.qualcomm.robotcore.hardware.DcMotorSimple;
        import com.qualcomm.robotcore.hardware.HardwareMap;
        import com.qualcomm.robotcore.util.ElapsedTime;
    """.trimIndent()
}
