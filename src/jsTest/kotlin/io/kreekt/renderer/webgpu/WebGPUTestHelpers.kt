package io.kreekt.renderer.webgpu

import io.kreekt.core.math.Matrix4
import io.kreekt.core.math.Vector3
import io.kreekt.core.scene.Mesh
import io.kreekt.geometry.BoxGeometry
import io.kreekt.material.MeshBasicMaterial
import kotlin.js.Json
import kotlin.js.json
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test Helper: Uniform Buffer State Capture
 * 
 * Tracks uniform buffer writes during rendering to verify
 * that each mesh gets unique uniform data.
 */
class UniformBufferCapture {
    data class UniformWrite(
        val offset: Int,
        val modelMatrix: FloatArray,
        val viewMatrix: FloatArray,
        val projectionMatrix: FloatArray
    )
    
    private val writes = mutableListOf<UniformWrite>()
    
    fun record(offset: Int, data: FloatArray) {
        // Uniform layout: proj(64) + view(64) + model(64) = 192 bytes
        val projMatrix = data.slice(0..15).toFloatArray()
        val viewMatrix = data.slice(16..31).toFloatArray()
        val modelMatrix = data.slice(32..47).toFloatArray()
        
        writes.add(UniformWrite(offset, modelMatrix, viewMatrix, projMatrix))
    }
    
    fun getAllWrites(): List<UniformWrite> = writes.toList()
    
    fun getWriteAtOffset(offset: Int): UniformWrite? = writes.find { it.offset == offset }
    
    fun clear() {
        writes.clear()
    }
}

/**
 * Test Helper: Bind Group State Capture
 * 
 * Tracks bind group creation and verifies unique offsets.
 */
class BindGroupCapture {
    data class BindGroupInfo(
        val bufferOffset: Int,
        val size: Int,
        val frameIndex: Int
    )
    
    private val bindGroups = mutableListOf<BindGroupInfo>()
    private var currentFrame = 0
    
    fun record(offset: Int, size: Int) {
        bindGroups.add(BindGroupInfo(offset, size, currentFrame))
    }
    
    fun getAllCreated(): List<BindGroupInfo> = bindGroups.toList()
    
    fun getOffsetsForLastFrame(): List<Int> {
        return bindGroups.filter { it.frameIndex == currentFrame }.map { it.bufferOffset }
    }
    
    fun nextFrame() {
        currentFrame++
    }
    
    fun clear() {
        bindGroups.clear()
        currentFrame = 0
    }
}

/**
 * Test Helper: Create test mesh at specific position
 */
fun createMeshAt(x: Double, y: Double, z: Double, id: String = "test-mesh"): Mesh {
    val geometry = BoxGeometry(1f, 1f, 1f)
    val material = MeshBasicMaterial()
    val mesh = Mesh(geometry, material)
    
    mesh.position.set(x.toFloat(), y.toFloat(), z.toFloat())
    mesh.updateMatrixWorld()
    
    return mesh
}

/**
 * Test Helper: Assert Matrix4 equality with tolerance
 */
fun assertMatrixEquals(expected: Matrix4, actual: Matrix4, tolerance: Double = 0.01, message: String = "") {
    for (i in 0..15) {
        val diff = kotlin.math.abs(expected.elements[i] - actual.elements[i])
        assertTrue(
            diff < tolerance,
            "$message Matrix element [$i] differs: expected ${expected.elements[i]}, actual ${actual.elements[i]}, diff=$diff"
        )
    }
}

/**
 * Test Helper: Assert FloatArray matrix equality with tolerance
 */
fun assertMatrixArrayEquals(expected: FloatArray, actual: FloatArray, tolerance: Float = 0.01f, message: String = "") {
    assertEquals(expected.size, actual.size, "$message Matrix array sizes differ")
    
    for (i in expected.indices) {
        val diff = kotlin.math.abs(expected[i] - actual[i])
        assertTrue(
            diff < tolerance,
            "$message Matrix element [$i] differs: expected ${expected[i]}, actual ${actual[i]}, diff=$diff"
        )
    }
}

/**
 * Test Helper: Create translation matrix
 */
fun translationMatrix(x: Double, y: Double, z: Double): Matrix4 {
    val matrix = Matrix4()
    matrix.makeTranslation(x, y, z)
    return matrix
}

/**
 * Test Helper: Extract model matrix from uniform data (FloatArray)
 */
fun extractModelMatrix(uniformData: FloatArray): FloatArray {
    // Uniform layout: proj(64 bytes / 16 floats) + view(64 bytes / 16 floats) + model(64 bytes / 16 floats)
    return uniformData.slice(32..47).toFloatArray()
}

/**
 * Test Helper: Extract translation from model matrix
 */
fun extractTranslation(modelMatrix: FloatArray): Vector3 {
    // Matrix4 in column-major: translation is at [12], [13], [14]
    return Vector3(
        modelMatrix[12].toDouble(),
        modelMatrix[13].toDouble(),
        modelMatrix[14].toDouble()
    )
}

/**
 * Test Helper: Check if uniform buffer contains expected translation at offset
 */
fun checkUniformBufferHasTranslation(
    buffer: dynamic,
    offset: Int,
    expectedX: Double,
    expectedY: Double,
    expectedZ: Double,
    tolerance: Double = 0.01
): Boolean {
    // Read model matrix from buffer at offset
    // Uniform layout: proj(64) + view(64) + model(64) = 192 bytes total per mesh
    val modelMatrixOffset = offset + 128 // Skip proj(64) + view(64)
    
    // Read translation values from buffer
    val tx: Float = js("buffer[modelMatrixOffset + 48]").unsafeCast<Float>() // element [12] * 4 bytes
    val ty: Float = js("buffer[modelMatrixOffset + 52]").unsafeCast<Float>() // element [13] * 4 bytes
    val tz: Float = js("buffer[modelMatrixOffset + 56]").unsafeCast<Float>() // element [14] * 4 bytes
    
    val diffX = kotlin.math.abs(tx - expectedX.toFloat())
    val diffY = kotlin.math.abs(ty - expectedY.toFloat())
    val diffZ = kotlin.math.abs(tz - expectedZ.toFloat())
    
    return diffX < tolerance && diffY < tolerance && diffZ < tolerance
}
