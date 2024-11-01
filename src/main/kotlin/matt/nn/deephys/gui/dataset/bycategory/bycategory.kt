package matt.nn.deephys.gui.dataset.bycategory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import matt.compose.controls.choicebox.MyChoiceBox
import matt.compose.graphics.text.MyText
import matt.lang.common.unsafeReturningErr
import matt.nn.deephys.gui.category.CategoryView
import matt.nn.deephys.gui.global.DeephysSpinner
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.prim.str.elementsToString

@Composable
fun ByCategoryView(
    testLoader: TypedTestLike<*>,
    viewer: DatasetViewerState,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {
    Column {

        @Suppress("USELESS_CAST")
        val cats = testLoader.test.categories.map { it as CategorySelection }

        Row {
            DeephysSpinner(
                label = "Category",
                choices = cats,
                defaultChoice = { viewer.categorySelection.value?.primaryCategory ?: cats[0] },
                converter = CategorySelection.stringConverterThatFallsBackToFirst(cats = cats.map { it as Category }),
                viewer = viewer,
                getCurrent = unsafeReturningErr { viewer.categorySelection },
                acceptIf = { it is Category },
                navAction = { navigateTo(it) },
                selected = unsafeReturningErr("?")
            )


            MyChoiceBox(
                selected = viewer.categorySelection.value,
                choices = testLoader.test.categories,
                labeler = {
                    when (it) {
                        is Category          -> it.label
                        is CategoryConfusion -> it.allCategories.map { it.label }.toList().elementsToString()
                        else                 -> "no category selected"
                    }
                },
                onChoose = {
                    viewer.navigateTo(it!!)
                }
            )
        }

        viewer.categorySelection.value?.let {
            CategoryView(it, testLoader = testLoader, viewer = viewer, settings = settings, viewerWidth = viewerWidth)
        } ?: MyText("select a category")
    }
}
