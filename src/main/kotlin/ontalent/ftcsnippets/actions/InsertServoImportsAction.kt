/** InsertServoImportsAction.kt  */

package ontalent.ftcsnippets.actions

class InsertServoImportsAction : AbstractInsertImportsAction(
    text = "Insert Servo Imports",
    description = "Inserts servo imports"
) {
    override fun getInsertText(): String = """
        import com.qualcomm.robotcore.hardware.CRServo;
        import com.qualcomm.robotcore.hardware.Servo;
    """.trimIndent()
}
