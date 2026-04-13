/** InsertSensorImportsAction.kt */

package ontalent.ftcsnippets.actions

class InsertSensorImportsAction : AbstractInsertImportsAction(
    text = "Insert Sensor Imports",
    description = "Inserts sensor imports"
) {
    override fun getInsertText(): String = """
        import com.qualcomm.robotcore.hardware.DistanceSensor;
        import com.qualcomm.robotcore.hardware.ColorSensor;
        import com.qualcomm.robotcore.hardware.TouchSensor;
        import com.qualcomm.robotcore.hardware.VoltageSensor;
    """.trimIndent()
}