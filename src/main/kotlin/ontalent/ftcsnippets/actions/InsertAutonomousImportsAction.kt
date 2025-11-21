//InsertAutonomousImportsAction.kt

package ontalent.ftcsnippets.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class InsertAutonomousImportsAction : AbstractInsertImportsAction() {

    override fun importsBlock(): String = """
        import com.qualcomm.robotcore.eventloop.opmode.OpMode;
        import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
        import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
    """.trimIndent()

    override fun classSkeleton(): String {
        // Try to grab file name from current PSI
        val dataContext = lastEvent?.getData(CommonDataKeys.PSI_FILE)
        val fileName = dataContext?.virtualFile?.nameWithoutExtension ?: "MyAutonomous"

        return """
            @Autonomous(name = "$fileName", group = "Autonomous")
            public class $fileName extends LinearOpMode { 
                
                @Override
                public void runOpMode() throws InterruptedException {
                    // TODO: Set up autonomous steps
                    
                    waitForStart();
                    
                    if (opModeIsActive()) {
                        // Your autonomous sequence
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
