/** InsertVisionsImportsAction.kt */

package ontalent.ftcsnippets.actions

class InsertVisionImportsAction : AbstractInsertImportsAction() {
    override fun importsBlock(): String = """
    import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
    import org.openftc.easyopencv.OpenCvCamera;
    import org.openftc.easyopencv.OpenCvCameraFactory;
    import org.openftc.easyopencv.OpenCvCameraRotation;
    import org.openftc.easyopencv.OpenCvPipeline;
    
    //This uses EasyOpenCV, click this link to learn more: https://gist.github.com/tinkerrc/12a7b5223df0cb55d7c1288ce96a6ab7
""".trimIndent()

}