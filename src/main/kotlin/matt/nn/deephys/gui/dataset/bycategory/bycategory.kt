package matt.nn.deephys.gui.dataset.bycategory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import matt.compose.controls.choicebox.MyChoiceBox
import matt.compose.controls.textfields.parsing.parser.SimpleBiTextParser
import matt.compose.graphics.text.MyText
import matt.lang.err.unsafeReturningErr
import matt.model.k.log.Logger
import matt.nn.deephys.gui.category.CategoryView
import matt.nn.deephys.gui.global.DeephysSpinner
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewerState
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.prim.exportfromlang.cfnf.Fail
import matt.prim.exportfromlang.generic.Failable
import matt.prim.str.join.elementsToString

@Composable
context(_: Logger)
fun ByCategoryView(
    testLoader: TypedTestLike<*>,
    viewer: DatasetViewerState,
    settings: DeephysSettingsController,
    viewerWidth: Dp
) {
    Column {

        @Suppress("USELESS_CAST")
        val cats = testLoader.test.categories.map { it as CategorySelection }

        @Suppress("ForbiddenIsCheck")
        Row {
            val converter = CategorySelection.stringConverterThatFallsBackToFirst(cats = cats.map { it as Category })
            DeephysSpinner(
                label = "Category",
                choices = cats,
                defaultChoice = { viewer.boundCategory.value?.primaryCategory ?: cats[0] },
                converter =
                    object: SimpleBiTextParser<CategorySelection> {
                        override fun rawInputOf(value: CategorySelection): String = converter.toString(value)

                        override fun tryParse(input: String): Failable<CategorySelection, Fail> = Failable.success(converter.fromString(input))
                    },
                viewer = viewer,
                getCurrent = unsafeReturningErr { viewer.boundCategory },
                acceptIf = { it is Category },
                navAction = { navigateTo(it) },
                selected = unsafeReturningErr("?")
            )

            MyChoiceBox(
                selected = viewer.boundCategory.value,
                choices = testLoader.test.categories,
                labeler = { categorySelection ->
                    when (categorySelection) {
                        is Category          -> categorySelection.label
                        is CategoryConfusion -> categorySelection.allCategories.map { it.label }.toList().elementsToString()
                        null                 -> "no category selected"
                    }
                },
                onChoose = {
                    viewer.navigateTo(it!!)
                }
            )
        }

        viewer.boundCategory.value?.let {
            CategoryView(it, testLoader = testLoader, viewer = viewer, settings = settings, viewerWidth = viewerWidth)
        } ?: MyText("select a category")
    }
}
