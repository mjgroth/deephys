@file:OptIn(UnsafeByteStringApi::class)

package matt.nn.deephys.load.cache.raf.deed

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.getByteString
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import matt.lang.sync.common.SimpleReferenceMonitor
import matt.lang.sync.common.withLock
import matt.nn.deephys.load.cache.raf.AsyncSparseWriter
import matt.nn.deephys.load.cache.raf.DeedKey
import matt.nn.deephys.load.cache.raf.RAFCacheImpl
import matt.nn.deephys.load.cache.raf.RAFLike
import matt.nn.deephys.load.cache.raf.SeekableRAFLike
import java.io.OutputStream
import java.nio.ByteBuffer

sealed interface Deed {
    fun write(bytes: ByteString)
    fun write(
        bytes: ByteString,
        destOffset: Int
    )

    fun write(bytes: ByteBuffer)
    fun write(
        bytes: ByteBuffer,
        destOffset: Int
    )

    fun outputStream(offset: Int = 0): OutputStream
    fun read(): ByteString
}

@OptIn(UnsafeByteStringApi::class)
class DeedImpl(
    private val startIndexInclusive: Long,
    val size: Int,
    private val rafCache: RAFCacheImpl,
    private val raf: () -> RAFLike,
    private val nextReader: () -> RAFLike,
    @Suppress("UNUSED_PARAMETER") key: DeedKey
) : Deed {
    val stopIndexExclusive: Long get() = startIndexInclusive + size

    @OptIn(UnsafeByteStringApi::class)
    override fun read(): ByteString {
        if (rafCache.closedWriting()) {
            val readerRAF = nextReader()
            val buff = ByteArray(size)
            readerRAF.monitor.withLock {
                readerRAF.apply {
                    readFully(startIndexInclusive, buff)
                }
            }
            return UnsafeByteStringOperations.wrapUnsafe(buff)
        }
        val buff = ByteArray(size)
        rafCache.monitor.withLock {
            raf().apply {
                readFully(startIndexInclusive, buff)
            }
        }
        return UnsafeByteStringOperations.wrapUnsafe(buff)
    }

    private inner class DeedOutputStream(private var offset: Int) : OutputStream() {
        private val monitor = SimpleReferenceMonitor()
        override fun write(b: Int) {
            monitor.withLock {
                this@DeedImpl.write(b, offset++)
            }
        }

        @OptIn(UnsafeByteStringApi::class)
        override fun write(
            b: ByteArray,
            off: Int,
            len: Int
        ) {
            monitor.withLock {
                /*The most unsafe ByteString wrap I have ever made*/
                this@DeedImpl.write(UnsafeByteStringOperations.wrapUnsafe(b), off, len, offset)
                offset += len
            }
        }
    }

    override fun outputStream(offset: Int): OutputStream = DeedOutputStream(offset)

    @Suppress("unused")
    fun write(byte: Byte) {
        raf().write(startIndexInclusive, byte.toInt())
    }

    @Suppress("unused")
    fun write(
        byte: Byte,
        destOffset: Int
    ) {
        raf().write(startIndexInclusive + destOffset, byte.toInt())
    }

    @Suppress("unused")
    fun write(byte: Int) {
        raf().write(startIndexInclusive, byte)
    }

    fun write(
        byte: Int,
        destOffset: Int
    ) {
        raf().write(startIndexInclusive + destOffset, byte)
    }

    override fun write(bytes: ByteString) {
        raf().write(startIndexInclusive, bytes)
    }

    override fun write(
        bytes: ByteString,
        destOffset: Int
    ) {
        raf().write(startIndexInclusive + destOffset, bytes)
    }

    fun write(
        bytes: ByteString,
        srcOffset: Int,
        srcLen: Int,
        destOffset: Int
    ) {
        UnsafeByteStringOperations.withByteArrayUnsafe(bytes) {
            raf().write(startIndexInclusive + destOffset, it, srcOffset, srcLen)
        }
    }

    override fun write(bytes: ByteBuffer) {
        val r = raf()
        when (r) {
            is SeekableRAFLike   -> {
                rafCache.monitor.withLock {
                    r.seek(startIndexInclusive)
                    r.channel.write(bytes)
                }
            }

            is AsyncSparseWriter -> {
                r.write(startIndexInclusive, bytes.getByteString())
            }
        }
    }

    override fun write(
        bytes: ByteBuffer,
        destOffset: Int
    ) {
        val r = raf()
        when (r) {
            is SeekableRAFLike   -> {
                rafCache.monitor.withLock {
                    r.seek(startIndexInclusive + destOffset)
                    r.channel.write(bytes)
                }
            }

            is AsyncSparseWriter -> {
                r.write(startIndexInclusive + destOffset, bytes.getByteString())
            }
        }
    }
}
