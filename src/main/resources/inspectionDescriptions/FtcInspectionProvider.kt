//FtcInspectionProvider

package ontalent.ftcsnippets.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.InspectionToolProvider

class FtcInspectionProvider : InspectionToolProvider {
    override fun getInspectionClasses(): Array<Class<out LocalInspectionTool>> {
        return arrayOf(FtcErrorInspection::class.java)
    }
}