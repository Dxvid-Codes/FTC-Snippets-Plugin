/** InsertServoImportsAction.kt  */

package ontalent.ftcsnippets.actions

class InsertServoImportsAction : AbstractInsertImportsAction() {
    override fun importsBlock(): String = """
        import com.qualcomm.robotcore.hardware.CRServo;
        import com.qualcomm.robotcore.hardware.Servo;
    """.trimIndent()
}
