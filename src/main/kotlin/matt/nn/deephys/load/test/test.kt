package matt.nn.deephys.load.test


import matt.async.thread.daemon
import matt.cbor.err.CborParseException
import matt.cbor.read.major.map.MapReader
import matt.cbor.read.major.txtstr.TextStringReader
import matt.cbor.read.streamman.cborReader
import matt.file.CborFile
import matt.lang.err
import matt.log.warn.warn
import matt.model.code.errreport.ThrowReport
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
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
import matt.nn.deephys.model.importformat.testlike.TestOrLoader
import matt.obs.col.olist.basicMutableObservableListOf
import matt.prim.str.elementsToString
import matt.prim.str.mybuild.string
import java.io.IOException

const val OLD_CAT_LOAD_WARNING = "Getting category the old way. This will fail if the image list didn't contain the category."

class TestLoader(
  file: CborFile,
  override val model: Model,
  settings: DeephysSettingsController
): AsyncLoader(file), TestOrLoader {


  override fun isDoneLoading(): Boolean {
	return finishedTest.isDone()
  }

  override val test get() = awaitFinishedTest()
  override val dtype get() = preppedTest.await().dtype
  private var finishedTest = LoadedValueSlot<Test<*>>()

  override val finishedLoadingAwaitable = finishedTest
  val imageSetLoader = ImageSetLoader(this)
  private val datasetHDCache = imageSetLoader.datasetHDCache
  fun awaitNonUniformRandomImage() = imageSetLoader.finishedImages.await().random()
  fun awaitImage(index: Int) = imageSetLoader.finishedImages.await()[index]
  fun awaitFinishedTest(): Test<*> = finishedTest.await()
  val numImages = LoadedValueSlot<ULong>()
  val loadedCategories = LoadedValueSlot<List<Category>>()
  val didLoadCategories = LoadedValueSlot<Boolean>()
  val progress by lazy { imageSetLoader.progress }
  val cacheProgressPixels by lazy { imageSetLoader.cacheProgressPixels }
  val cacheProgressActs by lazy { imageSetLoader.cacheProgressActs }


  fun category(id: Int): Category {

	val didLoadCats = didLoadCategories.await()
	if (didLoadCats) {
	  return loadedCategories.await()[id]
	} else {
	  warn(OLD_CAT_LOAD_WARNING)
	  return imageSetLoader.finishedImages.await().asSequence().map {
		it.category
	  }.first {
		it.id == id
	  }
	}
  }


  val infoString by lazy {
	string {
	  lineDelimited {
		+"Test:"
		+"\tname=${file.name}"
		+"\tnumImages=${numImages.await()}"
		+"\tpixelsShapePerImage=${imageSetLoader.pixelsShapePerImage.await().elementsToString()}"
		+"\tactivationsShapePerImage=${imageSetLoader.activationsShapePerImage.await().elementsToString()}"
	  }
	}
  }

  private enum class Keys(key: String? = null, val required: Boolean = false) {
	theName("name", required = true),
	suffix,
	dtype,
	classes,
	images(required = true);

	val key = key ?: name
  }

  val loadWarnings = basicMutableObservableListOf<String>()

  val start = SingleCall {
	daemon("TestLoader-${file.name}") {

	  if (!file.exists()) {
		signalFileNotFound()
		return@daemon
	  }
	  try {

		val stream = file.inputStream()
		try {
		  val reader = stream.cborReader()
		  reader.readManually<MapReader, Unit> {

			val keys = Keys.values()

			expectCount(keys.count { it.required }..keys.size)
			val countInt = count.toInt()

			var name: String? = null
			var dtype: DType<*> = Float32


			var imagesWereRead = false
			var catsWereRead = false


			repeat(countInt) { keyIdx: Int ->
			  println("keyIdx=$keyIdx")
			  val nextKey = nextKeyOrValueOnly<String>()
			  println("nextKey=$nextKey")

			  val theKey = keys.firstOrNull {
				it.key == nextKey
			  } ?: throw LoadException("Unknown Key: $nextKey")

			  when (theKey) {
				theName      -> {
				  name = nextKeyOrValueOnly()
				}

				Keys.suffix  -> {
				  loadWarnings += SUFFIX_WARNING
				  nextKeyOrValueOnly<String?>()
				}

				Keys.classes -> {
				  val cats = nextKeyOrValueOnly<List<String>>()
				  loadedCategories.putLoadedValue(cats.mapIndexed { idx, it ->
					Category(id = idx, label = it)
				  })
				  catsWereRead = true
				  didLoadCategories.putLoadedValue(true)
				}

				Keys.dtype   -> {
				  require(!imagesWereRead) {
					"Images must be read after the dtype"
				  }
				  dtype = nextValueManualDontReadKey<TextStringReader, DType<*>> {
					val str = this.read().raw
					when (str) {
					  "float32" -> Float32
					  "float64" -> Float64
					  else      -> err("str == $str")
					}
				  }
				}

				Keys.images  -> {
				  require(keyIdx == countInt - 1)
				  if (!catsWereRead) {
					loadWarnings += "You are using an old version of the python library which does not correctly save the list of classes. Please re-generate your data with the latest version from pip."
					didLoadCategories.putLoadedValue(false)
				  }
				  preppedTest.putLoadedValue(PreppedTestLoader(this@TestLoader, dtype))
				  imageSetLoader.readImages(
					reader = this,
					dtype = dtype,
					finishedTest = finishedTest
				  )
				  imagesWereRead = true
				}
			  }
			}




			imageSetLoader.neuronActCacheTools!!.forEach {
			  it.finalize()
			}
			datasetHDCache.neuronsRAF.closeWriting()

			progress.value = 1.0


			finishedTest.putLoadedValue(Test(
			  name = name ?: throw LoadException("Test did not have a name"),
			  images = imageSetLoader.finishedImages.await(), /*as List<DeephyImage<Float>>*/
			  model = this@TestLoader.model,
			  testRAMCache = testRAMCache,
			  dtype = dtype,
			  cats = if (didLoadCategories.await()) loadedCategories.await() else null
			).apply {
			  setTheTestNeurons(imageSetLoader.localTestNeurons)
			  /*testNeurons = localTestNeurons*/ /*as Map<InterTestNeuron,TestNeuron<Float>>*/
			  preds.startLoading()
			  startPreloadingMaxActivations()
			})
			println("load2")
			signalFinishedLoading()
		  }
		} catch (e: CborParseException) {
		  ThrowReport(Thread.currentThread(), e).print()
		  signalParseError(e)
		  return@daemon
		} catch (e: LoadException) {
		  ThrowReport(Thread.currentThread(), e).print()
		  signalParseError(e)
		  return@daemon
		}
		stream.close()
	  } catch (e: IOException) {
		ThrowReport(Thread.currentThread(), e).print()
		signalStreamNotOk()
	  }
	}
  }


  override val testRAMCache by lazy { TestRAMCache(settings) }


  val preppedTest = LoadedValueSlot<PreppedTestLoader<*>>()


}


class LoadException(message: String): Exception(message)
