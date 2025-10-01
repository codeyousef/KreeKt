package io.kreekt.lod

import io.kreekt.camera.Camera
import io.kreekt.core.scene.Object3D
import io.kreekt.core.math.Vector3

/**
 * Level of Detail - automatic mesh switching based on camera distance
 */
class LOD : Object3D() {
    private val levels = mutableListOf<LODLevel>()
    var autoUpdate: Boolean = true
    private var currentLevel: Int = -1

    data class LODLevel(
        val `object`: Object3D,
        val distance: Float
    )

    /**
     * Add detail level
     */
    fun addLevel(`object`: Object3D, distance: Float = 0f): LOD {
        levels.add(LODLevel(`object`, distance))
        levels.sortBy { it.distance }
        add(`object`)
        return this
    }

    /**
     * Get current active level
     */
    fun getCurrentLevel(): Int {
        return currentLevel
    }

    /**
     * Get levels
     */
    fun getLevels(): List<LODLevel> {
        return levels.toList()
    }

    /**
     * Update LOD based on camera distance
     */
    fun update(camera: Camera) {
        if (levels.size <= 1) {
            return
        }

        val distance = camera.position.distanceTo(getWorldPosition(Vector3()))

        levels.forEachIndexed { index, level ->
            level.`object`.visible = false
            if (index < levels.size - 1) {
                if (distance >= level.distance && distance < levels[index + 1].distance) {
                    level.`object`.visible = true
                    currentLevel = index
                }
            } else {
                if (distance >= level.distance) {
                    level.`object`.visible = true
                    currentLevel = index
                }
            }
        }
    }

    fun getObjectForDistance(distance: Float): Object3D? {
        for (i in 1 until levels.size) {
            if (distance < levels[i].distance) {
                return levels[i - 1].`object`
            }
        }
        return if (levels.isNotEmpty()) levels.last().`object` else null
    }
}