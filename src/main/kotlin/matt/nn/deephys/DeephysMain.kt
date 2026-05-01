package matt.nn.deephys

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.encodeToByteString
import matt.compose.controls.buttons.MyButton
import matt.compose.graphics.text.ErrorText
import matt.compose.state.shortcuts.rememberMutableStateOf
import matt.compose.state.shortcuts.rememberMutableStateOfFalse
import matt.exec.app.deephysSite
import matt.http.method.HTTPMethod.POST
import matt.http.tryHttp
import matt.lang.anno.Recycle
import matt.lang.controlflow.go
import matt.log.j.DefaultLogger
import matt.log.report.desktop.BugReport
import matt.model.code.args.Arguments
import matt.model.code.errreport.createThrowReport
import matt.model.context.SuspendingAutomationService
import matt.model.j.browse
import matt.model.k.osi.url.MURL
import matt.model.query.buildQueryURL
import matt.nn.deephys.gui.DeephysApp
import matt.nn.deephys.gui.DeephysArgs
import matt.nn.deephys.gui.settings.DeephySettingsNodeNode
import matt.nn.deephys.state.DeephyStateDb
import matt.osi.url.urlEncode
import matt.prim.exportfromlang.cfnf.getorthrow.getOrThrow
import matt.sys.shutdown.ShutdownExecutorImpl
import java.net.URI

fun main(args: Array<String>): Unit = Arguments.mainOrExitWithLogicalFailure<DeephysArgs>(args, ::main)

/*NOT INVOKED BY TEST in case I ever want the main test method to return something*/
@OptIn(ExperimentalCoroutinesApi::class)
fun main(args: DeephysArgs) {
    context(ShutdownExecutorImpl(), DefaultLogger) {
        runBlocking {
            val settingsNode = DeephySettingsNodeNode(this).dataObject.getCompleted().getOrThrow()
            DeephysApp().boot(
                args,
                settingsNode = settingsNode,
                deephyState = DeephyStateDb(this).dataObject.getCompleted().getOrThrow()
            )
        }
    }
}

@Suppress("unused")
@Recycle
@Composable
context(automationContext: SuspendingAutomationService)
fun SubmitBugReportButton(t: Thread, e: Exception) {
    val submitting = rememberMutableStateOfFalse()
    val scope = rememberCoroutineScope()
    val submittedUrl = rememberMutableStateOf<String?>(null)
    val gotErrorWhileSubmitting = rememberMutableStateOfFalse()
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
                            tryHttp(u) {
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
                    scope.launch {
                        automationContext.browse(URI(url))
                    }
                }
            }
        }
    }
}

@Suppress("unused")
@Recycle
context(automationContext: SuspendingAutomationService)
suspend fun openNewYouTrackIssue(
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
    automationContext.browse(u)
}
