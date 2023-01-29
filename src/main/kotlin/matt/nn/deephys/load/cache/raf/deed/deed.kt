package matt.nn.deephys.load.cache.raf.deed

import matt.nn.deephys.load.cache.raf.AsyncSparseWriter
import matt.nn.deephys.load.cache.raf.DeedKey
import matt.nn.deephys.load.cache.raf.RAFCacheImpl
import matt.nn.deephys.load.cache.raf.RAFLike
import matt.nn.deephys.load.cache.raf.SeekableRAFLike
import java.io.OutputStream
import java.nio.ByteBuffer

sealed interface Deed {
  fun write(bytes: ByteArray)
  fun write(bytes: ByteArray, destOffset: Int)
  fun write(bytes: ByteBuffer)
  fun write(bytes: ByteBuffer, destOffset: Int)
  fun outputStream(offset: Int = 0): OutputStream
  fun read(): ByteArray
}


class DeedImpl(
  private val startIndexInclusive: Long,
  val size: Int,
  private val rafCache: RAFCacheImpl,
  private val raf: ()->RAFLike,
  private val nextReader: ()->RAFLike,
  @Suppress("UNUSED_PARAMETER") key: DeedKey
): Deed {
  val stopIndexExclusive: Long get() = startIndexInclusive + size

  override fun read(): ByteArray {
	if (rafCache.closedWriting()) {
	  val readerRAF = nextReader()
	  val buff = ByteArray(size)
	  synchronized(readerRAF) {
		readerRAF.apply {
		  readFully(startIndexInclusive, buff)
		}
	  }
	  return buff
	} else {
	  val buff = ByteArray(size)
	  synchronized(rafCache) {
		raf().apply {
		  readFully(startIndexInclusive, buff)
		}
	  }
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

  override fun outputStream(offset: Int): OutputStream = DeedOutputStream(offset)

  fun write(byte: Byte) {
	raf().write(startIndexInclusive, byte.toInt())
  }

  fun write(byte: Byte, destOffset: Int) {
	raf().write(startIndexInclusive + destOffset, byte.toInt())
  }

  fun write(byte: Int) {
	raf().write(startIndexInclusive, byte)
  }

  fun write(byte: Int, destOffset: Int) {
	raf().write(startIndexInclusive + destOffset, byte)
  }

  override fun write(bytes: ByteArray) {
	raf().write(startIndexInclusive, bytes)
  }

  override fun write(bytes: ByteArray, destOffset: Int) {
	raf().write(startIndexInclusive + destOffset, bytes)
  }

  fun write(bytes: ByteArray, srcOffset: Int, srcLen: Int, destOffset: Int) {
	raf().write(startIndexInclusive + destOffset, bytes, srcOffset, srcLen)
  }

  override fun write(bytes: ByteBuffer) {
	val r = raf()
	when (r) {
	  is SeekableRAFLike   -> {
		synchronized(rafCache) {
		  r.seek(startIndexInclusive)
		  r.channel.write(bytes)
		}
	  }

	  is AsyncSparseWriter -> {
		r.write(startIndexInclusive, bytes.array().copyOf())
	  }
	}
  }

  override fun write(bytes: ByteBuffer, destOffset: Int) {
	val r = raf()
	when (r) {
	  is SeekableRAFLike   -> {
		synchronized(rafCache) {
		  r.seek(startIndexInclusive + destOffset)
		  r.channel.write(bytes)
		}
	  }

	  is AsyncSparseWriter -> {
		r.write(startIndexInclusive + destOffset, bytes.array().copyOf())
	  }
	}
  }
}