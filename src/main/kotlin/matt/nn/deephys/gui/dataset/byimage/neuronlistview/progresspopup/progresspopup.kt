package matt.nn.deephys.gui.dataset.byimage.neuronlistview.progresspopup

import javafx.geometry.Pos.CENTER
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.paint.Color
import javafx.stage.StageStyle.UNDECORATED
import matt.async.thread.queue.QueueWorker
import matt.fx.control.wrapper.label.LabelWrapper
import matt.fx.control.wrapper.progressbar.ProgressBarWrapper
import matt.fx.graphics.fxthread.ts.nonBlockingFXWatcher
import matt.fx.graphics.wrapper.pane.vbox.VBoxW
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapper
import matt.fx.graphics.style.border.FXBorder
import matt.fx.graphics.wrapper.stage.StageWrapper
import matt.fx.graphics.wrapper.window.WindowWrapper
import matt.gui.mscene.MScene
import matt.lang.function.Convert
import matt.lang.function.Produce
import matt.lang.go
import matt.nn.deephys.gui.dataset.byimage.neuronlistview.progresspopup.ProgressPopUp.Companion.worker
import matt.obs.math.double.op.times
import matt.obs.prop.SynchronizedProperty
import matt.time.dur.sleep
import kotlin.time.Duration.Companion.milliseconds

fun <R : Any> withProgressPopUp(op: Convert<ProgressTracker, R>): R {
    val tracker by lazy { ProgressTracker() }
    var r: R? = null
    worker.schedule {
        r = op(tracker)
        tracker.message = "done"
        tracker.progress = 1.0
    }
    r?.go { return it }
    (0..100).forEach { _ ->
        sleep(10.milliseconds)
        r?.go { return it }
    }
    val popup = tracker.with {
        if (tracker.progress < 1.0) {
            ProgressPopUp(tracker, owner = WindowWrapper.guessMainStage()!!)
        } else null
    }
    popup?.showAndWait()
    return r!!
}


class ProgressTracker {
    val progressProp = SynchronizedProperty(0.0)
    var progress by progressProp
    val messageProp = SynchronizedProperty("")
    var message by messageProp
    inline fun <R> with(crossinline op: Produce<R>) = progressProp.with<R> {
        messageProp.with {
            op()
        }
    }
}

private class ProgressPopUp(
    tracker: ProgressTracker = ProgressTracker(), owner: StageWrapper
) : StageWrapper(UNDECORATED) {

    companion object {
        internal val worker by lazy {
            QueueWorker()
        }
    }

    private val bar = ProgressBarWrapper().apply {
        progress = tracker.progress
    }
    private val label = LabelWrapper(tracker.message)


    init {
        initOwner(owner)

        width = 500.0
        height = 200.0


        val root = VBoxW().apply {
            border = FXBorder.solid(Color.BLUE)
            this.isFillWidth = true
            this.alignment = CENTER
            spacing = 20.0
            +this@ProgressPopUp.label
            +this@ProgressPopUp.bar.apply {
                vgrow = ALWAYS
                widthProperty
            }
        }
        scene = MScene<VBoxWrapper<*>>(root)

        bar.prefWidthProperty.bind(root.widthProperty * 0.8)
        bar.prefHeight = height / 4.0



        tracker.messageProp.nonBlockingFXWatcher().onChangeWithWeak(this) { dia, it ->
            dia.label.text = it
        }

        tracker.progressProp.nonBlockingFXWatcher().onChangeWithWeak(this) { dia, it ->
            dia.bar.progress = it
            if (it >= 1.0) {
                dia.close()
            }
        }


    }


}