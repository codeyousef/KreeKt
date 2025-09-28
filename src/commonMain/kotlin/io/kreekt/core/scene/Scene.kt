package io.kreekt.core.scene

import io.kreekt.core.math.Box3
import io.kreekt.core.math.Euler
import io.kreekt.core.math.Vector3
import io.kreekt.renderer.CubeTexture
import io.kreekt.renderer.Texture

/**
 * Scene represents the root of a 3D object hierarchy.
 * Compatible with Three.js Scene API.
 *
 * A Scene contains all the objects that should be rendered,
 * including meshes, lights, cameras, and other objects.
 * It also manages scene-wide properties like background and fog.
 */
class Scene : Object3D() {

    /**
     * Scene background - can be a color, texture, or gradient
     */
    var background: Background? = null

    /**
     * Environment map for image-based lighting
     */
    var environment: CubeTexture? = null

    /**
     * Scene fog for atmospheric effects
     */
    var fog: Fog? = null

    /**
     * Override material for all objects in the scene
     */
    var overrideMaterial: Material? = null

    /**
     * Whether to auto-update the scene
     */
    var autoUpdate: Boolean = true

    init {
        name = "Scene"
        // Scene has no parent and exists at world origin
        matrixAutoUpdate = false
        matrix.identity()
        matrixWorld.identity()
    }

    /**
     * Creates a copy of this scene
     */
    override fun clone(recursive: Boolean): Scene {
        return Scene().copy(this, recursive) as Scene
    }

    /**
     * Copies properties from another scene
     */
    override fun copy(source: Object3D, recursive: Boolean): Scene {
        super.copy(source, recursive)

        if (source is Scene) {
            background = source.background
            environment = source.environment
            fog = source.fog
            overrideMaterial = source.overrideMaterial
            autoUpdate = source.autoUpdate
        }

        return this
    }

    /**
     * Converts scene to JSON representation
     */
    fun toJSON(): SceneJSON {
        return SceneJSON(
            metadata = SceneMetadata(
                version = "4.0",
                type = "Object",
                generator = "KreeKt"
            ),
            background = background?.let { bg ->
                when (bg) {
                    is Background.Color -> mapOf("type" to "color", "color" to bg.color.getHex())
                    is Background.Texture -> mapOf("type" to "texture", "texture" to "cubeTexture")
                    is Background.Gradient -> mapOf(
                        "type" to "gradient",
                        "topColor" to bg.top.getHex(),
                        "bottomColor" to bg.bottom.getHex()
                    )
                }
            },
            fog = fog?.let { f ->
                when (f) {
                    is Fog.Linear -> mapOf(
                        "type" to "Fog",
                        "color" to f.color.getHex(),
                        "near" to f.near,
                        "far" to f.far
                    )
                    is Fog.Exponential -> mapOf(
                        "type" to "FogExp2",
                        "color" to f.color.getHex(),
                        "density" to f.density
                    )
                }
            },
`object` = toObject3DJSON()
        )
    }

    private fun toObject3DJSON(): Object3DJSON {
        return Object3DJSON(
            uuid = id.toString(),
            type = this::class.simpleName ?: "Object3D",
            name = name,
            matrix = matrix.elements.toList(),
            children = children.map { it.toObject3DJSON() }
        )
    }

    private fun Object3D.toObject3DJSON(): Object3DJSON {
        return Object3DJSON(
            uuid = id.toString(),
            type = this::class.simpleName ?: "Object3D",
            name = name,
            matrix = matrix.elements.toList(),
            children = children.map { it.toObject3DJSON() }
        )
    }
}

/**
 * Scene background types
 */
sealed class Background {
    /**
     * Solid color background
     */
    data class Color(val color: io.kreekt.core.math.Color) : Background()

    /**
     * Skybox/environment texture background
     */
    data class Texture(val texture: CubeTexture) : Background()

    /**
     * Gradient background
     */
    data class Gradient(val top: io.kreekt.core.math.Color, val bottom: io.kreekt.core.math.Color) : Background()
}

/**
 * Scene fog types
 */
sealed class Fog {
    /**
     * Linear fog with near and far distances
     */
    data class Linear(
        val color: io.kreekt.core.math.Color,
        val near: Float,
        val far: Float
    ) : Fog()

    /**
     * Exponential fog with density
     */
    data class Exponential(
        val color: io.kreekt.core.math.Color,
        val density: Float
    ) : Fog()
}

/**
 * JSON export data structures
 */
data class SceneJSON(
    val metadata: SceneMetadata,
    val background: Map<String, Any>? = null,
    val fog: Map<String, Any>? = null,
    val `object`: Object3DJSON? = null
) {
    // Use object3d to avoid Kotlin keyword conflict
    val object3d: Object3DJSON? = `object`
}

data class SceneMetadata(
    val version: String,
    val type: String,
    val generator: String
)

data class Object3DJSON(
    val uuid: String,
    val type: String,
    val name: String = "",
    val matrix: List<Float>,
    val children: List<Object3DJSON> = emptyList(),
    val visible: Boolean = true,
    val castShadow: Boolean = false,
    val receiveShadow: Boolean = false,
    val userData: Map<String, Any> = emptyMap()
)

/**
 * Base interface for texture types used in scene rendering
 */
interface Texture {
    val id: Int
    val name: String
    var needsUpdate: Boolean
}

interface CubeTexture : Texture

interface Material {
    val id: Int
    val name: String
    var needsUpdate: Boolean
    var visible: Boolean
}

/**
 * Scene builder DSL function
 */
fun scene(block: SceneBuilder.() -> Unit): Scene {
    return SceneBuilder().apply(block).build()
}

/**
 * Scene builder for DSL construction
 */
class SceneBuilder {
    private val scene = Scene()

    /**
     * Set scene background
     */
    fun background(color: io.kreekt.core.math.Color) {
        scene.background = Background.Color(color)
    }

    fun background(texture: CubeTexture) {
        scene.background = Background.Texture(texture)
    }

    fun gradientBackground(top: io.kreekt.core.math.Color, bottom: io.kreekt.core.math.Color) {
        scene.background = Background.Gradient(top, bottom)
    }

    /**
     * Set scene fog
     */
    fun fog(color: io.kreekt.core.math.Color, near: Float, far: Float) {
        scene.fog = Fog.Linear(color, near, far)
    }

    fun fogExp2(color: io.kreekt.core.math.Color, density: Float) {
        scene.fog = Fog.Exponential(color, density)
    }

    /**
     * Add objects to scene
     */
    fun add(vararg objects: Object3D) {
        scene.add(*objects)
    }

    /**
     * Create and add a group
     */
    fun group(name: String = "", block: GroupBuilder.() -> Unit): Group {
        val group = GroupBuilder(name).apply(block).build()
        scene.add(group)
        return group
    }

    internal fun build(): Scene = scene
}

/**
 * Group builder for hierarchical organization
 */
class GroupBuilder(private val groupName: String = "") {
    private val group = Group()

    init {
        group.name = groupName
    }

    /**
     * Set group position
     */
    fun position(x: Float, y: Float, z: Float) {
        group.position.set(x, y, z)
    }

    fun position(position: Vector3) {
        group.position.copy(position)
    }

    /**
     * Set group rotation
     */
    fun rotation(x: Float, y: Float, z: Float) {
        group.rotation.set(x, y, z)
    }

    fun rotation(rotation: Euler) {
        group.rotation.copy(rotation)
    }

    /**
     * Set group scale
     */
    fun scale(x: Float, y: Float, z: Float) {
        group.scale.set(x, y, z)
    }

    fun scale(scale: Vector3) {
        group.scale.copy(scale)
    }

    fun scale(uniformScale: Float) {
        group.scale.set(uniformScale, uniformScale, uniformScale)
    }

    /**
     * Add objects to group
     */
    fun add(vararg objects: Object3D) {
        group.add(*objects)
    }

    /**
     * Create nested groups
     */
    fun group(name: String = "", block: GroupBuilder.() -> Unit): Group {
        val childGroup = GroupBuilder(name).apply(block).build()
        group.add(childGroup)
        return childGroup
    }

    internal fun build(): Group = group
}

/**
 * Simple Group implementation
 */
class Group : Object3D() {

    init {
        name = "Group"
    }

    override fun clone(recursive: Boolean): Group {
        return Group().copy(this, recursive) as Group
    }

    override fun copy(source: Object3D, recursive: Boolean): Group {
        super.copy(source, recursive)
        return this
    }
}

/**
 * Scene utilities
 */
object SceneUtils {

    /**
     * Finds all objects of a specific type in the scene
     */
    inline fun <reified T : Object3D> findObjectsOfType(scene: Scene): List<T> {
        val results = mutableListOf<T>()
        scene.traverse { obj ->
            if (obj is T) {
                results.add(obj)
            }
        }
        return results
    }

    /**
     * Counts total objects in scene
     */
    fun countObjects(scene: Scene): Int {
        var count = 0
        scene.traverse { count++ }
        return count
    }

    /**
     * Gets bounding box of all objects in scene
     */
    fun getBoundingBox(scene: Scene): Box3 {
        val box = Box3()
        scene.traverse { obj ->
            // This would be expanded when geometry system is implemented
            // box.expandByObject(obj)
        }
        return box
    }

    /**
     * Gets center point of scene
     */
    fun getCenter(scene: Scene): Vector3 {
        return getBoundingBox(scene).getCenter()
    }

    /**
     * Creates a deep copy of the scene
     */
    fun deepClone(scene: Scene): Scene {
        return scene.clone(true)
    }
}

/**
 * Extension functions for Scene
 */

/**
 * Sets scene background color using RGB values
 */
fun Scene.setBackgroundColor(r: Float, g: Float, b: Float) {
    background = Background.Color(io.kreekt.core.math.Color(r, g, b))
}

/**
 * Sets scene background color using hex value
 */
fun Scene.setBackgroundColor(hex: Int) {
    background = Background.Color(io.kreekt.core.math.Color(hex))
}

/**
 * Adds linear fog to scene
 */
fun Scene.addFog(color: io.kreekt.core.math.Color, near: Float, far: Float) {
    fog = Fog.Linear(color, near, far)
}

/**
 * Adds exponential fog to scene
 */
fun Scene.addFogExp2(color: io.kreekt.core.math.Color, density: Float) {
    fog = Fog.Exponential(color, density)
}

/**
 * Removes fog from scene
 */
fun Scene.clearFog() {
    fog = null
}

/**
 * Checks if scene has fog
 */
fun Scene.hasFog(): Boolean = fog != null

/**
 * Checks if scene has background
 */
fun Scene.hasBackground(): Boolean = background != null