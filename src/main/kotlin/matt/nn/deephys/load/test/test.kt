package matt.nn.deephys.load.test


import matt.async.thread.daemon
import matt.cbor.err.CborParseException
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.major.txtstr.TextStringReader
import matt.cbor.read.streamman.cborReader
import matt.file.model.file.types.Cbor
import matt.file.model.file.types.TypedFile
import matt.file.toJioFile
import matt.lang.assertions.require.requireEquals
import matt.lang.assertions.require.requireNot
import matt.lang.common.err
import matt.log.warn.common.warn
import matt.model.code.errreport.j.ThrowReport
import matt.model.obj.single.SingleCall
import matt.nn.deephys.gui.global.tooltip.SUFFIX_WARNING
import matt.nn.deephys.gui.settings.DeephysSettingsController
import matt.nn.deephys.load.async.AsyncLoader
import matt.nn.deephys.load.test.TestLoader.Keys.theName
import matt.nn.deephys.load.test.dtype.DType
import matt.nn.deephys.load.test.dtype.Float32
import matt.nn.deephys.load.test.dtype.Float64
import matt.nn.deephys.load.test.imageloader.ImageSetLoader
import matt.nn.deephys.load.test.testcache.TestRAMCache
import matt.nn.deephys.load.test.testloadertwo.PreppedTestLoader
import matt.nn.deephys.model.data.Category
import matt.nn.deephys.model.importformat.Model
import matt.nn.deephys.model.importformat.Test
import matt.nn.deephys.model.importformat.im.DeephyImage
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.nn.deephys.model.importformat.testlike.TypedTestLike
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.prop.writable.BindableProperty
import matt.prim.str.elementsToString
import matt.prim.str.mybuild.api.string
import java.io.IOException

const val OLD_CAT_LOAD_WARNING =
    "Getting category the old way. This will fail if the image list didn't contain the category."

class TestLoader(
    file: TypedFile<Cbor, *>,
    override val model: Model,
    settings: DeephysSettingsController
) : AsyncLoader(file), TestOrLoader {


    override fun isDoneLoading(): Boolean =
        postDtypeTestLoader.getOrNullIfLoading()?.run {
            requireLoaded().isDoneLoading()
        } ?: false

    override val test get() = awaitFinishedTest()
    fun dtypeOrNull() = postDtypeTestLoader.awaitSuccessfulOrNull()?.dtype
    override val dtype get() = postDtypeTestLoader.awaitRequireSuccessful().dtype
    fun awaitFinishedTest() = postDtypeTestLoader.await().requireLoaded().awaitFinishedTest()

    val testName = DirectLoadedOrFailedValueSlot<String>()

    override val finishedLoadingAwaitable by lazy { postDtypeTestLoader.chainedTo { it.finishedTest } }

    val numImages = DirectLoadedOrFailedValueSlot<ULong>()
    val loadedCategories = DirectLoadedOrFailedValueSlot<List<Category>>()
    val didLoadCategories = DirectLoadedOrFailedValueSlot<Boolean>()


    var postDtypeTestLoader = DirectLoadedOrFailedValueSlot<PostDtypeTestLoader<*>>()

    fun category(id: Int): Category {

        val didLoadCats = didLoadCategories.awaitRequireSuccessful()
        if (didLoadCats) {
            return loadedCategories.awaitRequireSuccessful()[id]
        } else {
            warn(OLD_CAT_LOAD_WARNING)
            val finishedIms = postDtypeTestLoader.await().requireLoaded().imageSetLoader.finishedImages
            return finishedIms.awaitRequireSuccessful().asSequence().map {
                it.category
            }.first {
                it.id == id
            }
        }
    }


    val infoString by lazy {
        string {
            val isl = postDtypeTestLoader.await().requireLoaded().imageSetLoader
            lineDelimited {
                +"Test:"
                +"\tname=${file.name}"
                +"\tnumImages=${numImages.awaitSuccessfulOrMessage()}"
                +"\tpixelsShapePerImage=${
                    isl.pixelsShapePerImage.awaitSuccessfulOrNull()?.elementsToString()
                }"
                +"\tactivationsShapePerImage=${
                    isl.activationsShapePerImage.awaitSuccessfulOrNull()
                        ?.elementsToString()
                }"
            }
        }
    }

    private enum class Keys(
        key: String? = null,
        val required: Boolean = false
    ) {
        theName("name", required = true),
        suffix,
        dtype,
        classes,
        images(required = true);

        val key = key ?: name
    }


    val loadWarnings = basicMutableObservableListOf<String>()


    val start =
        SingleCall {
            daemon("TestLoader-${file.name}") {

                if (!file.toJioFile().exists()) {
                    signalFileNotFound()
                    return@daemon
                }
                try {

                    val stream = file.toJioFile().inputStream()
                    val reader = stream.cborReader()
                    reader.readManually<MapReader, Unit> {

                        val keys = Keys.entries

                        val list = listOf(true)
                        println(list.count { it } ..1)

                        expectCount(keys.count { it.required } ..keys.size)
                        val countInt = count.toInt()

                        var name: String? = null

                        var imagesWereRead = false
                        var catsWereRead = false


                        repeat(countInt) { keyIdx: Int ->
                            println("keyIdx=$keyIdx")
                            val nextKey = nextKeyOrValueOnly<String>()
                            println("nextKey=$nextKey")

                            val theKey =
                                keys.firstOrNull {
                                    it.key == nextKey
                                } ?: throw LoadException("Unknown Key: $nextKey")

                            when (theKey) {
                                theName      -> {
                                    name = nextKeyOrValueOnly()
                                    testName.putLoadedValue(name!!)
                                }

                                Keys.suffix  -> {
                                    loadWarnings += SUFFIX_WARNING
                                    nextKeyOrValueOnly<String?>()
                                }

                                Keys.classes -> {
                                    val cats = nextKeyOrValueOnly<List<String>>()
                                    loadedCategories.putLoadedValue(
                                        cats.mapIndexed { idx, it ->
                                            Category(id = idx, label = it)
                                        }
                                    )
                                    catsWereRead = true
                                    didLoadCategories.putLoadedValue(true)
                                }

                                Keys.dtype   -> {
                                    requireNot(imagesWereRead) {
                                        "Images must be read after the dtype"
                                    }

                                    val dtype =
                                        nextValueManualDontReadKey<TextStringReader, DType<*>> {
                                            when (val str = read().raw) {
                                                "float32" -> Float32
                                                "float64" -> Float64
                                                else      -> err("str == $str")
                                            }
                                        }
                                    check(!postDtypeTestLoader.isDone())
                                    postDtypeTestLoader.putLoadedValue(
                                        PostDtypeTestLoader(dtype, this@TestLoader)
                                    )
                                }

                                Keys.images  -> {

                                    requireEquals(keyIdx, countInt - 1)
                                    if (!catsWereRead) {

                                        loadWarnings +=
                                            "You are using an old version of the python library which does not correctly save the list of classes. Please re-generate your data with the latest version from pip."

                                        didLoadCategories.putLoadedValue(false)
                                    }
                                    if (!postDtypeTestLoader.isDone()) {
                                        postDtypeTestLoader.putLoadedValue(PostDtypeTestLoader(Float32, this@TestLoader))
                                    }
                                    val post = postDtypeTestLoader.getOrNullIfLoading()!!.requireLoaded()
                                    post.putPrepped()
                                    post.readImages(this)
                                    imagesWereRead = true
                                }
                            }
                        }


                        val thePost = postDtypeTestLoader.getOrNullIfLoading()!!.requireLoaded()



                        thePost.imageSetLoader.neuronActCacheTools!!.forEach {
                            it.finalize()
                        }
                        thePost.datasetHDCache.neuronsRAF.closeWriting()

                        progress.progress.value = 1.0

                        thePost.putTest(
                            name = name,
                            ramCache = testRAMCache,
                            cats = if (didLoadCategories.awaitRequireSuccessful()) loadedCategories.awaitRequireSuccessful() else null
                        )

                        println("load2")
                        signalFinishedLoading()
                    }
                    stream.close()
                } catch (e: IOException) {
                    ThrowReport(Thread.currentThread(), e).print()
                    signalStreamNotOk()
                } catch (e: CborParseException) {
                    ThrowReport(Thread.currentThread(), e).print()
                    signalParseError(e)
                    return@daemon
                } catch (e: LoadException) {
                    ThrowReport(Thread.currentThread(), e).print()
                    signalParseError(e)
                    return@daemon
                }
            }
        }


    override val testRAMCache by lazy { TestRAMCache(settings) }


    val progress = TestLoadingProgress()
}

class TestLoadingProgress {
    val progress by lazy { BindableProperty(0.0) }
    val cacheProgressPixels by lazy { BindableProperty(0.0) }
    val cacheProgressActs by lazy { BindableProperty(0.0) }
}

class PostDtypeTestLoader<D: Number>(
    override val dtype: DType<D>,
    private val testLoader: TestLoader
): TypedTestLike<D> {
    override val post = this
    fun <T> createSlot() = testLoader.DirectLoadedOrFailedValueSlot<T>()
    override fun numberOfImages(): ULong = testLoader.numImages.awaitRequireSuccessful()

    override fun imageAtIndex(i: Int): DeephyImage<D> = awaitImage(i)

    override val test: Test<D>
        get() = awaitFinishedTest()
    override val testRAMCache: TestRAMCache
        get() = testLoader.testRAMCache
    override val model: Model
        get() = testLoader.model

    override fun isDoneLoading(): Boolean = finishedTest.isDone()
    fun putPrepped() {
        preppedTest.putLoadedValue(PreppedTestLoader(this, dtype))
    }
    val preppedTest = testLoader.DirectLoadedOrFailedValueSlot<PreppedTestLoader<D>>()
    internal val finishedTest = testLoader.DirectLoadedOrFailedValueSlot<Test<D>>()
    val imageSetLoader = ImageSetLoader<D>(testLoader, this)
    internal val datasetHDCache = imageSetLoader.datasetHDCache
    fun awaitNonUniformRandomImage() = imageSetLoader.finishedImages.awaitRequireSuccessful().random()
    fun awaitImage(index: Int) = imageSetLoader.finishedImages.awaitRequireSuccessful()[index]
    fun awaitFinishedTest(): Test<D> = finishedTest.awaitRequireSuccessful()


    fun readImages(reader: MapReader) {
        imageSetLoader.readImages(
            reader = reader,
            dtype = dtype,
            finishedTest = finishedTest
        )
    }
    fun putTest(
        name: String?,
        ramCache: TestRAMCache,
        cats: List<Category>?
    ) {
        finishedTest.putLoadedValue(
            Test(
                name = name ?: throw LoadException("Test did not have a name"),
                images = imageSetLoader.finishedImages.awaitRequireSuccessful(), /*as List<DeephyImage<Float>>*/
                model = testLoader.model,
                testRAMCache = ramCache,
                dtype = dtype,
                cats = cats,
                post = this
            ).apply {

                putTestNeurons(imageSetLoader.localTestNeurons!!)
                /*testNeurons = localTestNeurons

                as Map<InterTestNeuron,TestNeuron<Float>>*/
                preds.startLoading()
                startPreloadingMaxActivations()
            }
        )
    }
}



class LoadException(message: String) : Exception(message)
