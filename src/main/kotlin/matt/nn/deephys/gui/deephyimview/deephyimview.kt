package matt.nn.deephys.gui.deephyimview

import javafx.scene.Cursor
import matt.async.thread.queue.pool.FakeWorkerPool
import matt.async.thread.queue.pool.QueueWorkerPool
import matt.file.ext.FileExtension
import matt.fx.graphics.dialog.saveFile
import matt.fx.graphics.fxthread.ensureInFXThreadOrRunLater
import matt.fx.graphics.wrapper.node.onLeftClick
import matt.fx.graphics.wrapper.style.toAwtColor
import matt.fx.node.proto.scaledcanvas.ScaledCanvas
import matt.gui.menu.context.mcontextmenu
import matt.image.desktop.save
import matt.lang.j.NUM_LOGICAL_CORES
import matt.log.warn.common.warn
import matt.nn.deephys.gui.draw.draw
import matt.nn.deephys.gui.global.tooltip.veryLazyDeephysTooltip
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.obs.math.double.op.div
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.awt.image.DataBufferInt
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import kotlin.time.Duration.Companion.milliseconds

class DeephyImView(
    im: DeephyImage<*>,
    viewer: DatasetViewer,
    big: Boolean = false,
    loadAsync: Boolean = false,
    settings: DeephysSettingsController
) : ScaledCanvas(
        initializeInLoadingMode = true,
        progressIndicatorWidthAndHeight = (if (big) viewer.bigImageScale.value else viewer.smallImageScale.value),
        delayLoadingIndicatorBy = 1000.milliseconds
    ) {

    companion object {
        val realPool = QueueWorkerPool(NUM_LOGICAL_CORES, "DeephyImView Worker")
        val fakePool = FakeWorkerPool() /*because of the flickering*/
        private var didWarnAboutCombiningMethods = false
    }

    private val weakViewer = viewer.weakRef
    val weakIm = im.weak

    init {

        val memSafeSettings = settings


        val localWeakIm = weakIm
        val localWeakViewer = weakViewer
        val weakThis = WeakReference(this)
        if (!didWarnAboutCombiningMethods) {
            warn("combine draw methods for V1 and deephy")
            didWarnAboutCombiningMethods = true
        }


        cursor = Cursor.HAND

        val pool = if (loadAsync) realPool else fakePool
        pool.schedule {
            val mat = im.matrix
            draw(mat)
            if (hoverProperty.value) {
                drawBorder()
            }
            hoverProperty.onChangeWithAlreadyWeak(localWeakIm) { deRefedIm, h ->
                if (h) weakThis.get()!!.drawBorder()
                else weakThis.get()!!.draw(deRefedIm)
            }

            onLeftClick {
                weakThis.get()!!.click()
            }
            mcontextmenu {
                onRequest {
                    "download image" does {
                        val pngFile =
                            saveFile(stage = weakThis.get()!!.stage) {
                                title = "choose where to save png"
                                extensionFilter(
                                    description = "png",
                                    FileExtension.PNG
                                )
                                initialSaveFileName =
                                    localWeakIm.deref()!!.category.label + "_" + localWeakIm.deref()!!.index.toString() + ".png"
                            }
                        if (pngFile != null) {

                            val mat2 = localWeakIm.deref()!!.matrix
                            val bi = BufferedImage(mat2.size, mat2[0].size, TYPE_INT_ARGB)
                            val pixelData = (bi.raster.dataBuffer as DataBufferInt).data

                            println("bi.width = ${bi.width}")
                            println("bi.height = ${bi.height}")
                            println("pixelData.length = ${pixelData.size}")

                            var i = 0
                            mat2.forEach {
                                it.forEach {
                                    val awt = it.toAwtColor()
                                    pixelData[i++] =
                                        ByteBuffer.wrap(
                                            byteArrayOf(
                                                awt.alpha.toByte(),
                                                awt.red.toByte(),
                                                awt.green.toByte(),
                                                awt.blue.toByte()
                                            )
                                        ).asIntBuffer().get()
                                }
                            }


                            require(pngFile.path.endsWith(".png"))
                            bi.save(pngFile)
                        }
                    }
                }
            }
            mat
        }.whenDone { mat ->
            ensureInFXThreadOrRunLater {
                showCanvas()
                veryLazyDeephysTooltip(
                    localWeakIm.deref()!!.category.label,
                    localWeakIm,
                    settings = memSafeSettings
                )
                val widthMaybe = mat[0].size.toDouble()
                if (big) {
                    scale.bindWeakly(localWeakViewer.deref()!!.bigImageScale / widthMaybe)
                } else {
                    scale.bindWeakly(localWeakViewer.deref()!!.smallImageScale / widthMaybe)
                }
            }
        }
    }

    fun click() {
        weakViewer.deref()!!.navigateTo(weakIm.deref()!!)
    }
}
