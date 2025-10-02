/**
 * Image-Based Lighting Processor - Facade
 * Delegates to modular implementation
 */
package io.kreekt.lighting

import io.kreekt.core.math.Color
import io.kreekt.core.math.Vector3
import io.kreekt.lighting.ibl.*
import io.kreekt.renderer.CubeTexture
import io.kreekt.renderer.Texture

// Re-export types from ibl package
typealias IBLResult<T> = io.kreekt.lighting.ibl.IBLResult<T>
typealias HDREnvironment = io.kreekt.lighting.ibl.HDREnvironment
typealias IBLConfig = io.kreekt.lighting.ibl.IBLConfig
typealias IBLEnvironmentMaps = io.kreekt.lighting.ibl.IBLEnvironmentMaps
typealias SphericalHarmonics = io.kreekt.lighting.ibl.SphericalHarmonics

/**
 * IBL Processor interface
 */
interface IBLProcessor {
    suspend fun generateEquirectangularMap(cubeMap: CubeTexture, width: Int, height: Int): Texture
    suspend fun generateIrradianceMap(environmentMap: Texture, size: Int): CubeTexture
    suspend fun generatePrefilterMap(environmentMap: Texture, size: Int, roughnessLevels: Int): CubeTexture
    fun generateBRDFLUT(size: Int): Texture
}

/**
 * Create default IBL processor instance
 */
fun createIBLProcessor(): IBLProcessor = IBLProcessorImpl()
