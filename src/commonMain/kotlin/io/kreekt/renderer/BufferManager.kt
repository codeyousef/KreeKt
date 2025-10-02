package io.kreekt.renderer

/**
 * Advanced buffer management system for GPU resources
 * T118 - Buffer management for vertex, index, uniform buffers with pooling
 *
 * This file serves as the public API entry point.
 * Implementation is in io.kreekt.renderer.buffer package.
 */

// Re-export all buffer types and interfaces
typealias BufferType = io.kreekt.renderer.buffer.BufferType
typealias BufferUsage = io.kreekt.renderer.buffer.BufferUsage
typealias BufferAccess = io.kreekt.renderer.buffer.BufferAccess
typealias AttributeFormat = io.kreekt.renderer.buffer.AttributeFormat
typealias IndexType = io.kreekt.renderer.buffer.IndexType
typealias VertexAttribute = io.kreekt.renderer.buffer.VertexAttribute
typealias BufferAllocationResult = io.kreekt.renderer.buffer.BufferAllocationResult
typealias BufferStats = io.kreekt.renderer.buffer.BufferStats
typealias Buffer = io.kreekt.renderer.buffer.Buffer
typealias VertexBuffer = io.kreekt.renderer.buffer.VertexBuffer
typealias IndexBuffer = io.kreekt.renderer.buffer.IndexBuffer
typealias UniformBuffer = io.kreekt.renderer.buffer.UniformBuffer
typealias StorageBuffer = io.kreekt.renderer.buffer.StorageBuffer
typealias BufferManager = io.kreekt.renderer.buffer.BufferManager
typealias DefaultBufferManager = io.kreekt.renderer.buffer.DefaultBufferManager
typealias BufferPool = io.kreekt.renderer.buffer.BufferPool
typealias BufferUtils = io.kreekt.renderer.buffer.BufferUtils
