@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package matt.nn.deephys

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.encodeToByteString
import matt.compose.controls.buttons.MyButton
import matt.compose.graphics.text.ErrorText
import matt.compose.state.rememberMutableStateOf
import matt.exec.app.deephysSite
import matt.http.method.HTTPMethod.POST
import matt.lang.anno.Recycle
import matt.lang.common.go
import matt.lang.context.AutomationContext
import matt.lang.j.browse
import matt.lang.model.url.MURL
import matt.lang.shutdown.j.ShutdownExecutorImpl
import matt.log.report.desktop.BugReport
import matt.model.code.args.Arguments
import matt.model.code.errreport.createThrowReport
import matt.model.code.successorfail.getOrThrow
import matt.model.query.buildQueryURL
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.DeephysArgs
import matt.osi.url.urlEncode
import java.net.URI

fun main(args: Array<String>): Unit = Arguments.mainOrExitWithLogicalFailure<DeephysArgs>(args, ::main)

/*NOT INVOKED BY TEST in case I ever want the main test method to return something*/
fun main(args: DeephysArgs) {
    with(ShutdownExecutorImpl()) {
        DeephysApp().boot(args)
    }
}



context(AutomationContext)
@Suppress("unused")
@Recycle
@Composable
fun SubmitBugReportButton(t: Thread, e: Exception) {
    val submitting = rememberMutableStateOf(false)
    val scope = rememberCoroutineScope()
    val submittedUrl = rememberMutableStateOf<String?>(null)
    val gotErrorWhileSubmitting = rememberMutableStateOf(false)
    if (gotErrorWhileSubmitting.value) {
        ErrorText("failed to submit. Please copy and paste the error and send to matt")
    } else {
        Column {
            MyButton(
                if (submittedUrl.value != null) "submitted"
                else if (submitting.value) "submitting (please wait)..."
                else "Submit Bug Report",
                enabled = !submitting.value
            ) {
                submitting.value = true
                scope.launch {
                    try {
                        val u = MURL(deephysSite)/*.productionHost*/ + "issue"
                        submittedUrl.value =
                            matt.http.tryHttp(u) {
                                method = POST
                                data = BugReport(t = t, e = e).text.encodeToByteString()
                            }.getOrThrow() /*FX IS DEAD*/.requireSuccessful().text()
                    } catch (e: Exception) {
                        createThrowReport(e, allowCapturingCurrentThread = true).print()
                        gotErrorWhileSubmitting.value = true
                    }
                }
            }
            submittedUrl.value?.go { url ->
                MyButton("view submitted bug") {
                    /*ON LINUX THIS MUST OCCUR IN ANOTHER THREAD*/
                    browse(URI(url))
                }
            }
        }
    }
}

context(AutomationContext)
@Suppress("unused")
@Recycle
fun openNewYouTrackIssue(
    summary: String,
    description: String
) {
    val u =
        buildQueryURL(
            "https://deephys.youtrack.cloud/newIssue",
            "project" to "D",
            "summary" to summary.urlEncode(),
            "description" to description.urlEncode()
        ).let {
            URI(it.path)
        }
    browse(u)
}

