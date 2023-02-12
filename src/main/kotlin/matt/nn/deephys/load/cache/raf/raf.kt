package matt.nn.deephys.load.cache.raf

import matt.async.thread.daemon
import matt.file.MFile
import matt.lang.NOT_IMPLEMENTED
import matt.lang.RUNTIME
import matt.lang.anno.SeeURL
import matt.lang.lastIndex
import matt.model.flowlogic.latch.SimpleLatch
import matt.nn.deephys.load.cache.raf.deed.Deed
import matt.nn.deephys.load.cache.raf.deed.DeedImpl
import matt.time.dur.sleep
import java.io.EOFException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.Channel
import java.nio.channels.CompletionHandler
import java.nio.channels.WritableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.SPARSE
import java.nio.file.StandardOpenOption.WRITE
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds


class EvenlySizedRAFCache(private val rafCache: RAFCacheImpl, private val deedSize: Int): RAFCache {
  constructor(f: MFile, deedSize: Int): this(rafCache = RAFCacheImpl(f), deedSize = deedSize)

  fun rent() = rafCache.rent(deedSize)
}


class RAFCacheImpl(
  private val f: MFile
): RAFCache {

  private var fact: Lazy<RAFLike> = lazy {
	/*AsyncSparseWriter(f)*/
	/*SparseWriter(f)*/
	RealRAF(RandomAccessFile(f, "rw"))
  }

  private val raf: RAFLike
	get() = fact.value

  //  private val channel: FileChannel get() = raf.channel

  private var didCloseWriting = false

  @Synchronized fun closeWriting() {
	if (didCloseWriting) return
	if (fact.isInitialized()) {
	  (raf as? AsyncSparseWriter)?.markFinishedWriting()
	  (raf as? AsyncSparseWriter)?.awaitAsyncOps()
	  raf.close()
	}
	fact = lazy {
	  RealRAF(RandomAccessFile(f, "r"))
	}
	didCloseWriting = true
  }

  fun closedWriting() = didCloseWriting


  private var lastDeed: Deed? = null
  private fun nextDeed(size: Int): Deed = lastDeed?.let {
	DeedImpl((it as DeedImpl).stopIndexExclusive, size, this, { raf }, { getNextReaderRAF() }, OnlyDeedKey)
  } ?: DeedImpl(0, size, this, { raf }, { getNextReaderRAF() }, OnlyDeedKey)

  private val agent = object {}
  fun rent(size: Int) = synchronized(agent) {
	nextDeed(size).also {
	  lastDeed = it
	}
  }


  protected fun finalize() {


	if (fact.isInitialized()) {
	  raf.close()
	}

  }

  private val readerRAFs by lazy {
	List(RUNTIME.availableProcessors()) {
	  lazy { RealRAF(RandomAccessFile(f, "r")) }
	}
  }
  private var nextReaderRAFI = 0

  @Synchronized
  private fun getNextReaderRAF(): RAFLike {
	val readRAF = readerRAFs[nextReaderRAFI].value
	if (nextReaderRAFI == readerRAFs.lastIndex) {
	  nextReaderRAFI = 0
	} else {
	  nextReaderRAFI++
	}
	return readRAF
  }

}

interface RAFCache

sealed interface DeedKey
private object OnlyDeedKey: DeedKey


sealed interface RAFLike {
  val channel: Channel
  fun write(byte: Int)
  fun write(pos: Long, byte: Int)
  fun readFully(buff: ByteArray)
  fun readFully(pos: Long, buff: ByteArray)
  fun write(bytes: ByteArray)
  fun write(pos: Long, bytes: ByteArray)
  fun write(bytes: ByteArray, srcOffset: Int, srcLen: Int)
  fun write(pos: Long, bytes: ByteArray, srcOffset: Int, srcLen: Int)
  fun close()
}

sealed class SeekableRAFLike: RAFLike {
  abstract override val channel: WritableByteChannel
  abstract fun seek(pos: Long)

  @Synchronized
  override fun write(pos: Long, byte: Int) {
	seek(pos)
	write(byte)
  }

  @Synchronized
  override fun readFully(pos: Long, buff: ByteArray) {
	seek(pos)
	readFully(buff)
  }

  @Synchronized
  override fun write(pos: Long, bytes: ByteArray) {
	seek(pos)
	write(bytes)
  }

  @Synchronized
  override fun write(pos: Long, bytes: ByteArray, srcOffset: Int, srcLen: Int) {
	seek(pos)
	write(bytes, srcOffset, srcLen)
  }
}

class RealRAF(private val raf: RandomAccessFile): SeekableRAFLike() {
  override val channel: WritableByteChannel
	get() = raf.channel

  override fun seek(pos: Long) {
	raf.seek(pos)
  }

  override fun write(byte: Int) {
	raf.write(byte)
  }


  override fun readFully(buff: ByteArray) {
	try {
	  raf.readFully(buff)
	} catch (e: EOFException) {
	  throw Exception("Could not read into buffer with length ${buff.size}", e)
	}
  }


  override fun write(bytes: ByteArray) {
	raf.write(bytes)
  }


  override fun close() {
	raf.close()
  }

  override fun write(bytes: ByteArray, srcOffset: Int, srcLen: Int) {
	raf.write(bytes, srcOffset, srcLen)
  }


}


@SeeURL("https://stackoverflow.com/questions/50191063/java-using-randomaccessfile-after-seek-is-very-slow-what-is-the-reason")
class SparseWriter(file: MFile): SeekableRAFLike() {

  companion object {
	private val options = arrayOf(
	  WRITE,
	  CREATE_NEW,
	  SPARSE
	)
  }

  override val channel by lazy {
	Files.newByteChannel(file.toPath(), *options)
  }

  override fun seek(pos: Long) {
	channel.position(pos)
  }

  override fun write(byte: Int) {
	channel.write(ByteBuffer.wrap(byteArrayOf(byte.toByte())))
  }

  override fun readFully(buff: ByteArray) {
	TODO("Not yet implemented")
  }

  override fun write(bytes: ByteArray) {
	channel.write(ByteBuffer.wrap(bytes))
  }

  override fun write(bytes: ByteArray, srcOffset: Int, srcLen: Int) {
	channel.write(ByteBuffer.wrap(bytes, srcOffset, srcLen))
  }


  override fun close() {
	channel.close()
  }
}

@SeeURL("https://stackoverflow.com/questions/50191063/java-using-randomaccessfile-after-seek-is-very-slow-what-is-the-reason")
class AsyncSparseWriter(@Suppress("UNUSED_PARAMETER") file: MFile): RAFLike {

  companion object {
	private val options = setOf(
	  WRITE,
	  CREATE_NEW,
	  SPARSE
	)
	private val pool by lazy { Executors.newFixedThreadPool(RUNTIME.availableProcessors()) }
	/*ThreadPoolExecutor(RUNTIME.availableProcessors(), RUNTIME.availableProcessors(), 1, TimeUnit.SECONDS)*/
  }

  init {
	error("obvious bug where bytes are being written to wrong pos. But might be supper fast if the async part is done right! Maybe try again another time")
  }

  private var startedWrites = AtomicInteger()
  private var finishedWrites = AtomicInteger()

  fun markFinishedWriting() {
	val l = SimpleLatch()
	latch = l
	fun done() = finishedWrites.get() == startedWrites.get()
	if (done()) {
	  latch!!.open()
	} else {
	  daemon(name = "markFinishedWriting") {
		var sleepTime = 10.milliseconds
		var i = 0
		while (l.isClosed) {
		  if (done()) l.open()
		  sleep(sleepTime)
		  i++
		  when (i) {
			10 -> sleepTime *= 10
			20 -> sleepTime *= 10
		  }
		}
	  }
	}
  }

  fun awaitAsyncOps() = latch!!.await()

  private var latch: SimpleLatch? = null

  override val channel by lazy {
	AsynchronousFileChannel.open(file.toPath(), options, pool)
  }


  override fun write(byte: Int) {
	NOT_IMPLEMENTED
  }

  private val handler by lazy {
	object: CompletionHandler<Int, Unit> {
	  override fun completed(result: Int, attachment: Unit?) {
		finishedWrites.incrementAndGet()
	  }

	  override fun failed(exc: Throwable, attachment: Unit?) {
		throw exc
	  }
	}
  }

  override fun write(pos: Long, byte: Int) {
	startedWrites.incrementAndGet()
	channel.write(ByteBuffer.wrap(byteArrayOf(byte.toByte())), pos, Unit, handler)
  }

  override fun write(bytes: ByteArray) {
	TODO("Not yet implemented")
  }

  override fun readFully(buff: ByteArray) {
	TODO("Not yet implemented")
  }

  override fun readFully(pos: Long, buff: ByteArray) {
	TODO("Not yet implemented")
  }

  override fun write(pos: Long, bytes: ByteArray) {
	startedWrites.incrementAndGet()
	channel.write(ByteBuffer.wrap(bytes), pos, Unit, handler)
  }

  override fun write(bytes: ByteArray, srcOffset: Int, srcLen: Int) {
	NOT_IMPLEMENTED
  }

  override fun write(pos: Long, bytes: ByteArray, srcOffset: Int, srcLen: Int) {
	startedWrites.incrementAndGet()
	channel.write(ByteBuffer.wrap(bytes, srcOffset, srcLen), pos, Unit, handler)
  }

  override fun close() {
	channel.close()
  }
}