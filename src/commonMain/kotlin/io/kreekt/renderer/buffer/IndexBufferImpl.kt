package io.kreekt.renderer.buffer

import io.kreekt.renderer.RendererException
import io.kreekt.renderer.RendererResult

/**
 * Default index buffer implementation
 */
internal class DefaultIndexBuffer(
    override val id: Int,
    override val size: Long,
    override val usage: BufferUsage,
    override val indexType: IndexType
) : IndexBuffer {

    override val type: BufferType = BufferType.INDEX
    override val access: BufferAccess = BufferAccess.WRITE_ONLY
    override var needsUpdate: Boolean = false
    override val count: Int get() = (size / indexType.size).toInt()

    private var data: ByteArray? = null
    private var isMapped = false

    override fun uploadData(data: ByteArray, offset: Long): RendererResult<Unit> {
        return try {
            this.data = data.copyOf()
            needsUpdate = true
            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Upload failed", e))
        }
    }

    override fun uploadFloatData(data: FloatArray, offset: Long): RendererResult<Unit> {
        return RendererResult.Error(RendererException.InvalidState("Cannot upload float data to index buffer"))
    }

    override fun uploadIntData(data: IntArray, offset: Long): RendererResult<Unit> {
        val byteArray = when (indexType) {
            IndexType.UINT16 -> {
                ByteArray(data.size * 2).apply {
                    for (i in data.indices) {
                        val value = data[i]
                        this[i * 2] = (value and 0xFF).toByte()
                        this[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
                    }
                }
            }
            IndexType.UINT32 -> {
                ByteArray(data.size * 4).apply {
                    for (i in data.indices) {
                        val value = data[i]
                        this[i * 4] = (value and 0xFF).toByte()
                        this[i * 4 + 1] = ((value shr 8) and 0xFF).toByte()
                        this[i * 4 + 2] = ((value shr 16) and 0xFF).toByte()
                        this[i * 4 + 3] = ((value shr 24) and 0xFF).toByte()
                    }
                }
            }
        }
        return uploadData(byteArray, offset)
    }

    override fun map(access: BufferAccess): ByteArray? {
        if (isMapped) return null
        isMapped = true
        return data?.copyOf()
    }

    override fun unmap() {
        isMapped = false
    }

    override fun copyFrom(source: Buffer, srcOffset: Long, dstOffset: Long, size: Long): RendererResult<Unit> {
        return try {
            val sourceData = source.map(BufferAccess.READ_ONLY)
                ?: return RendererResult.Error(RendererException.InvalidState("Source buffer has no data or cannot be mapped"))

            val actualSize = if (size == -1L) {
                (sourceData.size - srcOffset).coerceAtLeast(0)
            } else {
                size
            }

            if (srcOffset < 0 || dstOffset < 0 || actualSize < 0) {
                return RendererResult.Error(RendererException.InvalidState("Invalid offset or size parameters"))
            }

            if (srcOffset + actualSize > sourceData.size) {
                return RendererResult.Error(RendererException.InvalidState("Source copy bounds exceed buffer size"))
            }

            if (dstOffset + actualSize > this.size) {
                return RendererResult.Error(RendererException.InvalidState("Destination copy bounds exceed buffer size"))
            }

            if (this.data == null) {
                this.data = ByteArray(this.size.toInt())
            }

            sourceData.copyInto(
                destination = this.data!!,
                destinationOffset = dstOffset.toInt(),
                startIndex = srcOffset.toInt(),
                endIndex = (srcOffset + actualSize).toInt()
            )

            needsUpdate = true
            source.unmap()

            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.InvalidState("Buffer copy failed: ${e.message}"))
        }
    }

    override fun bind(): RendererResult<Unit> {
        return RendererResult.Success(Unit)
    }

    override fun dispose() {
        data = null
        isMapped = false
    }
}
