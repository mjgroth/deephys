package matt.nn.deephys.load.cache.raf

import matt.file.MFile
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


@Suppress("BlockingMethodInNonBlockingContext") class RAFCache(
  private val f: MFile
) {

  //  companion object {
  //	val instances = mutableListOf<RAFCache>()
  //  }

  //  init {
  //	instances += this
  //  }

  private var fact = lazy {
	RandomAccessFile(f, "rw")
  }
  private val raf: RandomAccessFile
	get() = fact.value

  private val channel: FileChannel get() = raf.channel

  private var didCloseWriting = false

  @Synchronized fun closeWriting() {
	if (didCloseWriting) return
	if (fact.isInitialized()) {
	  raf.close()
	}
	fact = lazy {
	  RandomAccessFile(f, "r")
	}
	didCloseWriting = true
  }

  //  val profSeek = ProfiledBlock["seek-${f.abspath}"]
  //  val profWriteBytes = ProfiledBlock["writeBytes-${f.abspath}"]

  private inner class DeedImpl(
	val startIndexInclusive: Long, val size: Int
  ): Deed {
	val stopIndexExclusive: Long get() = startIndexInclusive + size

	override fun read(): ByteArray {
	  synchronized(this@RAFCache) {
		val buff = ByteArray(size)
		raf.seek(startIndexInclusive)
		raf.readFully(buff)
		return buff
	  }
	}

	private inner class DeedOutputStream(private var offset: Int): OutputStream() {
	  @Synchronized override fun write(b: Int) {
		this@DeedImpl.write(b, offset++)
	  }

	  @Synchronized override fun write(b: ByteArray, off: Int, len: Int) {
		this@DeedImpl.write(b, off, len, offset)
		offset += len
	  }
	}

	override fun outputStream(offset: Int) = DeedOutputStream(offset)

	fun write(byte: Byte) {
	  raf.seek(startIndexInclusive)
	  raf.write(byte.toInt())
	}

	fun write(byte: Byte, destOffset: Int) {
	  raf.seek(startIndexInclusive + destOffset)
	  raf.write(byte.toInt())
	}

	fun write(byte: Int) {
	  raf.seek(startIndexInclusive)
	  raf.write(byte)
	}

	fun write(byte: Int, destOffset: Int) {
	  raf.seek(startIndexInclusive + destOffset)
	  raf.write(byte)
	}

	override fun write(bytes: ByteArray) {
	  synchronized(this@RAFCache) {
		raf.seek(startIndexInclusive)
		raf.write(bytes)
	  }
	}

	override fun write(bytes: ByteArray, destOffset: Int) {
	  synchronized(this@RAFCache) {
		raf.seek(startIndexInclusive + destOffset)
		raf.write(bytes)
	  }
	}

	fun write(bytes: ByteArray, srcOffset: Int, srcLen: Int, destOffset: Int) {
	  synchronized(this@RAFCache) {
		raf.seek(startIndexInclusive + destOffset)
		raf.write(bytes, srcOffset, srcLen)
	  }
	}

	override fun write(bytes: ByteBuffer) {
	  synchronized(this@RAFCache) {
		//		profSeek.with {
		raf.seek(startIndexInclusive)
		//		}
		//		profWriteBytes.with {
		channel.write(bytes)
		//		}
	  }
	}

	override fun write(bytes: ByteBuffer, destOffset: Int) {
	  synchronized(this@RAFCache) {
		//		profSeek.with {
		raf.seek(startIndexInclusive + destOffset)
		//		}
		//		profWriteBytes.with {
		channel.write(bytes)
		//		}
	  }
	}
  }

  private var lastDeed: DeedImpl? = null
  private fun nextDeed(size: Int) = lastDeed?.let {
	DeedImpl(it.stopIndexExclusive, size)
  } ?: DeedImpl(0, size)

  @Synchronized fun rent(size: Int): Deed = nextDeed(size).also {
	lastDeed = it
  }


  protected fun finalize() {


	if (fact.isInitialized()) {
	  raf.close()
	}

  }
}

sealed interface Deed {
  fun write(bytes: ByteArray)
  fun write(bytes: ByteArray, destOffset: Int)
  fun write(bytes: ByteBuffer)
  fun write(bytes: ByteBuffer, destOffset: Int)
  fun outputStream(offset: Int = 0): OutputStream
  fun read(): ByteArray
}