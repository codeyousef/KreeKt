/**
 * T011-T012: WebGPUBufferManager Implementation
 * Feature: 020-go-from-mvp
 *
 * WebGPU buffer management for vertex, index, and uniform buffers.
 */

package io.kreekt.renderer.webgpu

import io.kreekt.renderer.feature020.*
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint32Array

/**
 * WebGPU buffer manager implementation.
 *
 * Manages GPU buffer lifecycle using GPUBuffer and device.queue.writeBuffer().
 *
 * @property device WebGPU device
 */
class WebGPUBufferManager(
    private val device: dynamic // GPUDevice
) : BufferManager {

    // Track destroyed buffers to prevent double-destroy
    private val destroyedBuffers = mutableSetOf<dynamic>()

    /**
     * Create vertex buffer from float array.
     *
     * Process:
     * 1. Convert FloatArray to Float32Array
     * 2. Create GPUBuffer with usage VERTEX | COPY_DST
     * 3. Write data via device.queue.writeBuffer()
     *
     * @param data Vertex data (position + color, interleaved)
     * @return Buffer handle with GPUBuffer
     * @throws IllegalArgumentException if data is empty
     * @throws OutOfMemoryException if allocation fails
     */
    override fun createVertexBuffer(data: FloatArray): BufferHandle {
        if (data.isEmpty()) {
            throw IllegalArgumentException("Vertex data cannot be empty")
        }

        // Data must be multiple of 6 floats (position + color)
        if (data.size % 6 != 0) {
            throw IllegalArgumentException(
                "vertexData.size must be multiple of 6 (position + color), got ${data.size}"
            )
        }

        val sizeBytes = data.size * 4 // 4 bytes per float

        return try {
            // Convert FloatArray to Float32Array
            val float32Array = Float32Array(data.size)
            for (i in data.indices) {
                float32Array.asDynamic()[i] = data[i]
            }

            // Create GPUBuffer
            val buffer = device.createBuffer(
                js(
                    """({
                size: sizeBytes,
                usage: GPUBufferUsage.VERTEX | GPUBufferUsage.COPY_DST,
                mappedAtCreation: false
            })"""
                )
            )

            if (buffer == null || buffer == undefined) {
                throw OutOfMemoryException("Failed to create vertex buffer")
            }

            // Write data to buffer
            device.queue.writeBuffer(buffer, 0, float32Array.buffer, 0, sizeBytes)

            BufferHandle(
                handle = buffer,
                size = sizeBytes,
                usage = BufferUsage.VERTEX
            )
        } catch (e: OutOfMemoryException) {
            throw e
        } catch (e: Exception) {
            throw OutOfMemoryException("Unexpected error creating vertex buffer: ${e.message}")
        } catch (e: Throwable) {
            throw OutOfMemoryException("Unexpected error creating vertex buffer: ${e.message}")
        }
    }

    /**
     * Create index buffer from int array.
     *
     * @param data Triangle indices (must be multiple of 3)
     * @return Buffer handle with GPUBuffer
     * @throws IllegalArgumentException if data is empty or not triangles
     */
    override fun createIndexBuffer(data: IntArray): BufferHandle {
        if (data.isEmpty()) {
            throw IllegalArgumentException("Index data cannot be empty")
        }

        if (data.size % 3 != 0) {
            throw IllegalArgumentException(
                "indexData.size must be multiple of 3 (triangles), got ${data.size}"
            )
        }

        val sizeBytes = data.size * 4 // 4 bytes per uint32

        return try {
            // Convert IntArray to Uint32Array
            val uint32Array = Uint32Array(data.size)
            for (i in data.indices) {
                uint32Array.asDynamic()[i] = data[i]
            }

            // Create GPUBuffer
            val buffer = device.createBuffer(
                js(
                    """({
                size: sizeBytes,
                usage: GPUBufferUsage.INDEX | GPUBufferUsage.COPY_DST,
                mappedAtCreation: false
            })"""
                )
            )

            if (buffer == null || buffer == undefined) {
                throw OutOfMemoryException("Failed to create index buffer")
            }

            // Write data to buffer
            device.queue.writeBuffer(buffer, 0, uint32Array.buffer, 0, sizeBytes)

            BufferHandle(
                handle = buffer,
                size = sizeBytes,
                usage = BufferUsage.INDEX
            )
        } catch (e: OutOfMemoryException) {
            throw e
        } catch (e: Exception) {
            throw OutOfMemoryException("Unexpected error creating index buffer: ${e.message}")
        } catch (e: Throwable) {
            throw OutOfMemoryException("Unexpected error creating index buffer: ${e.message}")
        }
    }

    /**
     * Create uniform buffer with fixed size.
     *
     * @param sizeBytes Buffer size in bytes (minimum 64 for mat4x4)
     * @return Buffer handle with GPUBuffer
     * @throws IllegalArgumentException if sizeBytes < 64
     */
    override fun createUniformBuffer(sizeBytes: Int): BufferHandle {
        if (sizeBytes < 64) {
            throw IllegalArgumentException(
                "uniformBuffer.sizeBytes must be at least 64 bytes (mat4x4), got $sizeBytes"
            )
        }

        return try {
            // Create GPUBuffer
            val buffer = device.createBuffer(
                js(
                    """({
                size: sizeBytes,
                usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST,
                mappedAtCreation: false
            })"""
                )
            )

            if (buffer == null || buffer == undefined) {
                throw OutOfMemoryException("Failed to create uniform buffer")
            }

            BufferHandle(
                handle = buffer,
                size = sizeBytes,
                usage = BufferUsage.UNIFORM
            )
        } catch (e: OutOfMemoryException) {
            throw e
        } catch (e: Exception) {
            throw OutOfMemoryException("Unexpected error creating uniform buffer: ${e.message}")
        } catch (e: Throwable) {
            throw OutOfMemoryException("Unexpected error creating uniform buffer: ${e.message}")
        }
    }

    /**
     * Update uniform buffer data (transformation matrices).
     *
     * @param handle Buffer handle from createUniformBuffer()
     * @param data Matrix data as byte array (64 bytes for mat4x4)
     * @param offset Write offset in bytes (must be 16-byte aligned)
     * @throws InvalidBufferException if handle is invalid or destroyed
     * @throws IllegalArgumentException if offset not aligned or data too large
     */
    override fun updateUniformBuffer(handle: BufferHandle, data: ByteArray, offset: Int) {
        // Validate handle
        if (!handle.isValid()) {
            throw InvalidBufferException("Buffer handle is invalid (null handle or zero size)")
        }

        val buffer = handle.handle
            ?: throw InvalidBufferException("Buffer handle is null")

        // Check if destroyed
        if (destroyedBuffers.contains(buffer)) {
            throw InvalidBufferException("Buffer has been destroyed")
        }

        // Validate offset alignment (16-byte for mat4x4)
        if (offset % 16 != 0) {
            throw IllegalArgumentException("offset must be 16-byte aligned, got $offset")
        }

        // Validate data size
        if (offset + data.size > handle.size) {
            throw IllegalArgumentException(
                "data too large: offset=$offset + size=${data.size} > buffer.size=${handle.size}"
            )
        }

        try {
            // Convert ByteArray to Int8Array
            val int8Array = Int8Array(data.size)
            for (i in data.indices) {
                int8Array.asDynamic()[i] = data[i]
            }

            // Write data to buffer
            device.queue.writeBuffer(buffer, offset, int8Array.buffer, 0, data.size)
        } catch (e: Exception) {
            throw InvalidBufferException("Failed to update uniform buffer: ${e.message}")
        } catch (e: Throwable) {
            throw InvalidBufferException("Failed to update uniform buffer: ${e.message}")
        }
    }

    /**
     * Destroy buffer and release GPU memory.
     *
     * @param handle Buffer handle to destroy
     * @throws InvalidBufferException if handle already destroyed
     */
    override fun destroyBuffer(handle: BufferHandle) {
        val buffer = handle.handle
            ?: throw InvalidBufferException("Buffer handle is null")

        // Check if already destroyed
        if (destroyedBuffers.contains(buffer)) {
            throw InvalidBufferException("Buffer has already been destroyed")
        }

        try {
            // Destroy buffer
            buffer.asDynamic().destroy()

            // Mark as destroyed
            destroyedBuffers.add(buffer)
        } catch (e: Exception) {
            throw InvalidBufferException("Failed to destroy buffer: ${e.message}")
        } catch (e: Throwable) {
            throw InvalidBufferException("Failed to destroy buffer: ${e.message}")
        }
    }
}
