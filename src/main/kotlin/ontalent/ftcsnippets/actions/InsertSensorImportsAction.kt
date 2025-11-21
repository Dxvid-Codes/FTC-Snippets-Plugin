/** InsertSensorImportsAction.kt */

package ontalent.ftcsnippets

class InsertSensorImportsAction : AbstractInsertImportsAction() {
    override fun importsBlock(): String = """
        import com.qualcomm.robotcore.hardware.DistanceSensor;
        import com.qualcomm.robotcore.hardware.ColorSensor;
        import com.qualcomm.robotcore.hardware.TouchSensor;
        import com.qualcomm.robotcore.hardware.VoltageSensor;
    """.trimIndent()
}
