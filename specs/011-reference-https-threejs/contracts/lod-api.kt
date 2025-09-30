/**
 * Level of Detail (LOD) API Contract
 * Maps FR-LO001 through FR-LO005
 *
 * Constitutional Requirements:
 * - Efficient LOD switching based on distance
 * - Type-safe LOD level management
 * - Performance: <1ms LOD selection per frame
 */

package io.kreekt.lod

import io.kreekt.core.Object3D
import io.kreekt.camera.Camera
import io.kreekt.math.Vector3
import io.kreekt.geometry.BufferGeometry

/**
 * FR-LO001, FR-LO002: Level of Detail container
 *
 * Test Contract:
 * - MUST switch between detail levels based on distance
 * - MUST support multiple LOD levels
 * - MUST compute distance from camera
 * - MUST render only one level at a time
 */
class LOD : Object3D() {
    /**
     * LOD levels in order of decreasing detail
     */
    private val levels: MutableList<LODLevel> = mutableListOf()

    /**
     * Auto-update LOD based on camera distance
     * Set to false for manual control
     */
    var autoUpdate: Boolean = true

    /**
     * FR-LO003: Add LOD level
     *
     * @param object Object to display at this level
     * @param distance Distance from camera at which to switch to this level
     *
     * Test Contract:
     * - MUST add object as child
     * - MUST store distance threshold
     * - MUST maintain sorted order by distance
     * - Distance 0 = highest detail (always shown up close)
     * - Higher distances = lower detail
     */
    fun addLevel(obj: Object3D, distance: Float = 0f): LOD {
        levels.add(LODLevel(obj, distance))
        levels.sortBy { it.distance }
        add(obj)
        return this
    }

    /**
     * Remove LOD level
     *
     * @param object Object to remove
     * @return true if removed
     */
    fun removeLevel(obj: Object3D): Boolean {
        val level = levels.find { it.`object` === obj }
        if (level != null) {
            levels.remove(level)
            remove(obj)
            return true
        }
        return false
    }

    /**
     * Get LOD level by distance
     */
    fun getLevelByDistance(distance: Float): Object3D? {
        return levels.lastOrNull { it.distance <= distance }?.`object`
    }

    /**
     * Get currently active LOD level
     */
    fun getCurrentLevel(): Int

    /**
     * Get all LOD levels
     */
    fun getLevels(): List<LODLevel> = levels.toList()

    /**
     * FR-LO004: Update LOD based on camera
     *
     * @param camera Camera to compute distance from
     *
     * Test Contract:
     * - MUST compute distance from camera to LOD center
     * - MUST select appropriate level based on distance
     * - MUST show/hide objects for selected level
     * - MUST be called automatically if autoUpdate=true
     */
    fun update(camera: Camera) {
        if (!visible) return

        val distance = getDistanceToCamera(camera)
        var currentLevel = -1

        for (i in levels.indices.reversed()) {
            if (distance >= levels[i].distance) {
                currentLevel = i
                break
            }
        }

        // Show selected level, hide others
        for (i in levels.indices) {
            levels[i].`object`.visible = (i == currentLevel)
        }
    }

    /**
     * Compute distance from camera to LOD center
     *
     * Test Contract:
     * - MUST compute world position
     * - MUST account for camera position
     */
    private fun getDistanceToCamera(camera: Camera): Float {
        val worldPosition = Vector3()
        getWorldPosition(worldPosition)

        val cameraPosition = Vector3()
        camera.getWorldPosition(cameraPosition)

        return worldPosition.distanceTo(cameraPosition)
    }

    /**
     * Get raycasting LOD (closest level for picking)
     */
    fun getRaycastLOD(): Object3D? {
        return levels.firstOrNull()?.`object`
    }

    /**
     * Clone LOD with all levels
     */
    override fun clone(): LOD

    /**
     * Convert to JSON
     */
    fun toJSON(): LODJson
}

/**
 * LOD level data
 */
data class LODLevel(
    val `object`: Object3D,
    val distance: Float
)

/**
 * LOD JSON representation
 */
data class LODJson(
    val levels: List<LODLevelJson>
)

data class LODLevelJson(
    val objectUuid: String,
    val distance: Float
)

/**
 * FR-LO005: LOD utilities
 */
object LODUtils {
    /**
     * Create LOD hierarchy for mesh
     *
     * @param highDetailMesh Highest detail mesh
     * @param distances Distance thresholds for each level
     * @param decimationRatios Triangle reduction ratio per level (0-1)
     *
     * Test Contract:
     * - MUST generate simplified meshes
     * - MUST set appropriate distances
     * - MUST preserve UVs and materials
     */
    fun createLODHierarchy(
        highDetailMesh: Mesh,
        distances: List<Float>,
        decimationRatios: List<Float>
    ): LOD

    /**
     * Compute optimal LOD distances based on screen coverage
     *
     * @param boundingRadius Object bounding sphere radius
     * @param screenCoverageThresholds Fraction of screen at each level (0-1)
     * @param fov Camera field of view in radians
     * @param screenHeight Screen height in pixels
     *
     * Test Contract:
     * - MUST compute distance for given screen coverage
     * - Formula: distance = (boundingRadius * screenHeight) / (2 * coverage * tan(fov/2))
     */
    fun computeOptimalDistances(
        boundingRadius: Float,
        screenCoverageThresholds: List<Float>,
        fov: Float,
        screenHeight: Int
    ): List<Float>

    /**
     * Simplify geometry for LOD level
     *
     * @param geometry Source geometry
     * @param targetRatio Target triangle count ratio (0-1)
     * @param preserveBoundary Preserve boundary edges
     *
     * Test Contract:
     * - MUST reduce triangle count
     * - MUST preserve overall shape
     * - MUST maintain UV seams
     * - MUST use quadric error metrics
     */
    fun simplifyGeometry(
        geometry: BufferGeometry,
        targetRatio: Float,
        preserveBoundary: Boolean = true
    ): BufferGeometry

    /**
     * Generate LOD chain from single mesh
     *
     * @param mesh Source mesh
     * @param levelCount Number of LOD levels
     * @param minDistance Minimum viewing distance
     * @param maxDistance Maximum viewing distance
     *
     * Test Contract:
     * - MUST create levelCount LOD levels
     * - MUST space distances logarithmically
     * - MUST simplify progressively
     */
    fun generateLODChain(
        mesh: Mesh,
        levelCount: Int = 4,
        minDistance: Float = 0f,
        maxDistance: Float = 1000f
    ): LOD

    /**
     * Compute LOD transition distances for smooth blending
     *
     * @param distances LOD switch distances
     * @param hysteresis Hysteresis factor to prevent popping (0-1)
     *
     * Test Contract:
     * - MUST add hysteresis to avoid rapid switching
     * - MUST return up/down transition thresholds
     */
    fun computeHysteresisDistances(
        distances: List<Float>,
        hysteresis: Float = 0.1f
    ): List<Pair<Float, Float>>

    /**
     * Analyze LOD performance
     *
     * @param lod LOD object to analyze
     * @return Performance metrics for each level
     */
    fun analyzeLODPerformance(lod: LOD): LODPerformanceMetrics

    /**
     * Export LOD to file format
     */
    fun exportLOD(lod: LOD): LODExportData

    /**
     * Import LOD from file format
     */
    fun importLOD(data: LODExportData): LOD
}

/**
 * LOD performance metrics
 */
data class LODPerformanceMetrics(
    val levels: List<LODLevelMetrics>
)

data class LODLevelMetrics(
    val distance: Float,
    val triangleCount: Int,
    val vertexCount: Int,
    val memoryUsageBytes: Int,
    val estimatedDrawTimeMs: Float
)

/**
 * LOD export data
 */
data class LODExportData(
    val levels: List<LODLevelExport>
)

data class LODLevelExport(
    val distance: Float,
    val geometry: BufferGeometry,
    val materialIndex: Int
)

/**
 * Automatic LOD manager for scene-wide LOD management
 *
 * Test Contract:
 * - MUST update all LODs in scene
 * - MUST batch updates for efficiency
 * - MUST respect LOD.autoUpdate flags
 */
class LODManager {
    private val managedLODs: MutableList<LOD> = mutableListOf()

    /**
     * Register LOD for management
     */
    fun addLOD(lod: LOD) {
        if (lod !in managedLODs) {
            managedLODs.add(lod)
        }
    }

    /**
     * Unregister LOD
     */
    fun removeLOD(lod: LOD) {
        managedLODs.remove(lod)
    }

    /**
     * Update all managed LODs
     *
     * @param camera Camera to compute distances from
     *
     * Test Contract:
     * - MUST update only LODs with autoUpdate=true
     * - MUST be efficient for many LODs
     * - Target: <1ms for 100 LODs
     */
    fun update(camera: Camera) {
        managedLODs.forEach { lod ->
            if (lod.autoUpdate) {
                lod.update(camera)
            }
        }
    }

    /**
     * Clear all managed LODs
     */
    fun clear() {
        managedLODs.clear()
    }

    /**
     * Get statistics
     */
    fun getStats(): LODManagerStats
}

data class LODManagerStats(
    val totalLODs: Int,
    val activeLODs: Int,
    val totalLevels: Int,
    val averageLevelsPerLOD: Float
)

// Forward declarations
expect class Mesh