/**
 * Asset Exporter System Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Asset Export
 *
 * These contracts define the API surface for exporting scenes and geometries
 * to various industry-standard formats.
 *
 * Requirements covered: FR-029 through FR-035
 */

package io.kreekt.exporter

import io.kreekt.core.Result
import io.kreekt.core.scene.Scene
import io.kreekt.geometry.BufferGeometry

/**
 * Base interface for all asset exporters.
 *
 * FR-035: All exporters MUST support platform-appropriate file writing mechanisms
 */
interface AssetExporter {
    /**
     * Exports a scene to a byte array.
     *
     * @param scene The scene to export
     * @param options Export options
     * @return Result containing exported data
     */
    suspend fun export(
        scene: Scene,
        options: ExportOptions = ExportOptions()
    ): Result<ByteArray>

    /**
     * Exports a scene directly to a file.
     *
     * @param scene The scene to export
     * @param path File path
     * @param options Export options
     * @return Result indicating success or failure
     */
    suspend fun exportToFile(
        scene: Scene,
        path: String,
        options: ExportOptions = ExportOptions()
    ): Result<Unit>
}

/**
 * Common export options.
 */
data class ExportOptions(
    /**
     * Export as binary format (vs text).
     */
    val binary: Boolean = false,

    /**
     * Embed textures in output file.
     */
    val embedTextures: Boolean = false,

    /**
     * Include animations.
     */
    val includeAnimations: Boolean = true,

    /**
     * Include cameras.
     */
    val includeCameras: Boolean = true,

    /**
     * Include lights.
     */
    val includeLights: Boolean = true,

    /**
     * Maximum texture size (downscale if larger).
     */
    val maxTextureSize: Int = 4096,

    /**
     * Compression level (0-10, higher = better compression).
     */
    val compressionLevel: Int = 5,

    /**
     * Only export visible objects.
     */
    val onlyVisible: Boolean = false,

    /**
     * Truncate draw range to actual geometry bounds.
     */
    val truncateDrawRange: Boolean = true
)

/**
 * Exports scenes to GLTF 2.0 format.
 *
 * FR-029: System MUST provide GLTFExporter to export scenes and objects to GLTF/GLB format
 *
 * @sample
 * ```kotlin
 * val exporter = GLTFExporter()
 * val gltfData = exporter.export(scene, ExportOptions(
 *     binary = true,
 *     embedTextures = true
 * )).getOrThrow()
 *
 * // Write to file
 * exporter.exportToFile(scene, "scene.glb")
 * ```
 */
class GLTFExporter : AssetExporter {
    override suspend fun export(
        scene: Scene,
        options: ExportOptions
    ): Result<ByteArray>

    override suspend fun exportToFile(
        scene: Scene,
        path: String,
        options: ExportOptions
    ): Result<Unit>

    /**
     * Exports a single object (not full scene).
     */
    suspend fun exportObject(
        obj: io.kreekt.core.scene.Object3D,
        options: ExportOptions = ExportOptions()
    ): Result<ByteArray>
}

/**
 * Exports scenes to USDZ format (Apple AR).
 *
 * FR-030: System MUST provide USDZExporter to export scenes to USDZ format for Apple AR
 *
 * @sample
 * ```kotlin
 * val exporter = USDZExporter()
 * val usdzData = exporter.export(scene).getOrThrow()
 * // Compatible with AR Quick Look on iOS
 * ```
 */
class USDZExporter : AssetExporter {
    override suspend fun export(
        scene: Scene,
        options: ExportOptions
    ): Result<ByteArray>

    override suspend fun exportToFile(
        scene: Scene,
        path: String,
        options: ExportOptions
    ): Result<Unit>
}

/**
 * Exports geometries to OBJ format.
 *
 * FR-031: System MUST provide OBJExporter to export geometries to OBJ format
 *
 * @sample
 * ```kotlin
 * val exporter = OBJExporter()
 * val objText = exporter.exportGeometry(geometry).getOrThrow()
 * ```
 */
class OBJExporter {
    /**
     * Exports a single geometry to OBJ format.
     *
     * @return OBJ text content
     */
    suspend fun exportGeometry(
        geometry: BufferGeometry
    ): Result<String>

    /**
     * Exports a scene to OBJ format.
     *
     * @return OBJ text content
     */
    suspend fun exportScene(
        scene: Scene,
        options: ExportOptions = ExportOptions()
    ): Result<String>

    /**
     * Exports to file.
     */
    suspend fun exportToFile(
        scene: Scene,
        path: String,
        options: ExportOptions = ExportOptions()
    ): Result<Unit>
}

/**
 * Exports geometries to PLY format.
 *
 * FR-032: System MUST provide PLYExporter to export geometries to PLY format
 */
class PLYExporter {
    /**
     * Exports a geometry to PLY format (binary or ASCII).
     */
    suspend fun exportGeometry(
        geometry: BufferGeometry,
        binary: Boolean = false
    ): Result<ByteArray>

    /**
     * Exports to file.
     */
    suspend fun exportToFile(
        geometry: BufferGeometry,
        path: String,
        binary: Boolean = false
    ): Result<Unit>
}

/**
 * Exports geometries to STL format (3D printing).
 *
 * FR-033: System MUST provide STLExporter to export geometries to STL format for 3D printing
 */
class STLExporter {
    /**
     * Exports a geometry to STL format (binary or ASCII).
     */
    suspend fun exportGeometry(
        geometry: BufferGeometry,
        binary: Boolean = true
    ): Result<ByteArray>

    /**
     * Exports to file.
     */
    suspend fun exportToFile(
        geometry: BufferGeometry,
        path: String,
        binary: Boolean = true
    ): Result<Unit>
}

/**
 * Exports scenes to COLLADA format.
 *
 * FR-034: System MUST provide ColladaExporter to export scenes to COLLADA format
 */
class ColladaExporter : AssetExporter {
    override suspend fun export(
        scene: Scene,
        options: ExportOptions
    ): Result<ByteArray>

    override suspend fun exportToFile(
        scene: Scene,
        path: String,
        options: ExportOptions
    ): Result<Unit>
}
