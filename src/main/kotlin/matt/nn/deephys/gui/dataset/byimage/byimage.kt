package matt.nn.deephys.gui.dataset.byimage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import matt.compose.graphics.text.MyText
import matt.lang.common.go
import matt.lang.common.unsafeErr
import matt.lang.common.unsafeReturningErr
import matt.lang.weak.weak
import matt.nn.deephys.calc.ImageTopPredictions
import matt.nn.deephys.gui.dataset.byimage.feat.FeaturesView
import matt.nn.deephys.gui.dataset.byimage.preds.PredictionsView
import matt.nn.deephys.gui.deephyimview.DeephyImView
import matt.nn.deephys.gui.global.DeephysSpinner
import matt.nn.deephys.gui.global.SpacerWithOldFxSize
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.load.test.testloadertwo.PreppedTestLoader
import matt.nn.deephys.model.importformat.im.DeephyImage


@Composable
fun <A: Number> ByImageView(
    testLoader: PreppedTestLoader<A>,
    viewer: DatasetViewerState,
    settings: DeephysSettingsController
) {
    Column {
        val weakViewer = weak(viewer)

        val images = testLoader.test.images

        DeephysSpinner(
            label = "Image",
            choices = images,
            defaultChoice = { images[0] },
            converter = DeephyImage.stringConverterThatFallsBackToFirst(images = images),
            viewer = viewer,
            getCurrent = unsafeReturningErr { viewer.imageSelection },
            acceptIf = { true },
            navAction = { navigateTo(it) },
            selected = unsafeReturningErr("?")
        )

        val img = viewer.imageSelection.value
        if (viewer.isUnboundToDSet.value && img != null) {
            weakViewer.deref()?.let { deRefedViewer ->
                Row {
                    Column {
                        DeephyImView(img, deRefedViewer, big = true, settings = settings)
                    }
                    Spacer(Modifier.size(10.dp))
                    PredictionsView(
                        img.category,
                        ImageTopPredictions(img),
                        weakViewer,
                        settings
                    )
                    SpacerWithOldFxSize()
                    img.features?.takeIf { it.isNotEmpty() }?.go {
                        FeaturesView(it)
                    }
                }
            } ?: MyText("if you see this, then there must be a problem")
        } else MyText("no image selected")
        Spacer(Modifier.size(10.dp))
        unsafeErr(
            """
            neuronListViewSwapper(
                viewer = viewer,
                top = viewer.topNeurons,
                bindScrolling = true,
                settings = memSafeSettings
            )      
            """.trimIndent()
        )
    }
}
