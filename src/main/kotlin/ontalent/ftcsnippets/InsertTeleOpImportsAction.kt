package ontalent.ftcsnippets

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class InsertTeleOpImportsAction : AbstractInsertImportsAction() {

    override fun importsBlock(): String = """
        import com.qualcomm.robotcore.eventloop.opmode.OpMode;
        import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
        import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
    """.trimIndent()

    override fun classSkeleton(): String {
        // Try to grab file name from current PSI
        val dataContext = lastEvent?.getData(CommonDataKeys.PSI_FILE)
        val fileName = dataContext?.virtualFile?.nameWithoutExtension ?: "MyTeleOp"

        return """
            @TeleOp(name = "$fileName", group = "TeleOp")
            public class $fileName extends LinearOpMode { 
                
                @Override
                public void runOpMode() throws InterruptedException {
                    // TODO: Essentially your main method
                    
                    waitForStart();
                    
                    while(opModeIsActive()) {
                        // Your loop
                    }
                }
            }
        """.trimIndent()
    }

    // Store last event for file name lookup
    private var lastEvent: AnActionEvent? = null
    override fun update(e: AnActionEvent) {
        lastEvent = e
        super.update(e)
    }
}
