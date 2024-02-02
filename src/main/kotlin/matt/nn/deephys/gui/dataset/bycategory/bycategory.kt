package matt.nn.deephys.gui.dataset.bycategory

import matt.fx.base.converter.toFXConverter
import matt.fx.control.wrapper.control.choice.choicebox
import matt.fx.graphics.wrapper.pane.anchor.swapper.swapper
import matt.fx.graphics.wrapper.pane.hbox.h
import matt.fx.graphics.wrapper.pane.vbox.VBoxWrapperImpl
import matt.fx.graphics.wrapper.region.RegionWrapper
import matt.lang.go
import matt.lang.weak.MyWeakRef
import matt.lang.weak.WeakRefInter
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.model.op.convert.toStringConverter
import matt.nn.deephys.gui.category.CategoryView
import matt.nn.deephys.gui.dataset.MainDeephysView
import matt.nn.deephys.gui.global.DEEPHYS_FADE_DUR
import matt.nn.deephys.gui.global.configForDeephys
import matt.nn.deephys.gui.global.deephysSpinner
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.gui.viewer.DatasetViewer
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.data.CategoryConfusion
import matt.nn.deephys.model.data.CategorySelection
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import matt.prim.str.elementsToString

class ByCategoryView(
    testLoader: TypedTestLike<*>,
    viewer: DatasetViewer,
    override val settings: DeephysSettingsController
): VBoxWrapperImpl<RegionWrapper<*>>(), MainDeephysView {

    override val control: ObsVal<WeakRefInter<RegionWrapper<*>>?> = BindableProperty(null)

    init {


        @Suppress("USELESS_CAST")
        val cats = testLoader.test.categories.map { it as CategorySelection }


        h {
            (this@ByCategoryView.control as BindableProperty<WeakRefInter<RegionWrapper<*>>?>).value = MyWeakRef(this)
            deephysSpinner(
                label = "Category",
                choices = cats,
                defaultChoice = { viewer.categorySelection.value?.primaryCategory ?: cats[0] },
                converter = CategorySelection.stringConverterThatFallsBackToFirst(cats = cats.map { it as Category }),
                viewer = viewer,
                getCurrent = viewer.categorySelection,
                acceptIf = { it is Category },
                navAction = { navigateTo(it) }
            )


            choicebox(
                nullableProp = BindableProperty(viewer.categorySelection.value),
                values = testLoader.test.categories
            ) {
                configForDeephys()
                converter = toStringConverter<CategorySelection?> {
                    when (it) {
                        is Category -> it.label
                        is CategoryConfusion -> it.allCategories.map { it.label }.toList().elementsToString()
                        else -> "no category selected"
                    }
                }.toFXConverter()


                val rBlocker = RecursionBlocker()

                selectedItemProperty.onChangeWithWeak(viewer) { deRefedViewer, selection ->
                    selection?.go {
                        rBlocker.with {
                            deRefedViewer.navigateTo(it)
                        }
                    }
                }

                viewer.categorySelection.onChange {
                    rBlocker.with {
                        select(it)
                    }
                }


            }
        }

        swapper(
            viewer.categorySelection,
            nullMessage = "select a category",
            fadeOutDur = DEEPHYS_FADE_DUR,
            fadeInDur = DEEPHYS_FADE_DUR
        ) {
            CategoryView(this, testLoader = testLoader, viewer = viewer, settings = settings)
        }
    }
}
