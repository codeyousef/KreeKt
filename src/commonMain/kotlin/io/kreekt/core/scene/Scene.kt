package io.kreekt.core.scene

import io.kreekt.core.math.Box3
import io.kreekt.core.math.Euler
import io.kreekt.core.math.Vector3
import io.kreekt.renderer.CubeTexture
import io.kreekt.renderer.Texture

/**
 * # Scene - Root Container for 3D Content
 *
 * The scene is the root of the object hierarchy and the primary container for all renderable
 * content in KreeKt. It manages scene-wide properties like background, fog, and environment
 * lighting while serving as the starting point for rendering.
 *
 * ## Overview
 *
 * A Scene provides:
 * - **Object Container**: Hierarchical organization of meshes, lights, and cameras
 * - **Visual Environment**: Background colors, textures, or gradients
 * - **Atmospheric Effects**: Fog for depth cueing and atmosphere
 * - **Global Lighting**: Environment maps for image-based lighting (IBL)
 * - **Material Override**: Global material replacement for special rendering passes
 *
 * ## Basic Usage
 *
 * ```kotlin
 * // Create a scene with background and fog
 * val scene = Scene().apply {
 *     background = Background.Color(Color(0x87CEEB)) // Sky blue
 *     fog = Fog.Linear(
 *         color = Color(0xCCCCCC),
 *         near = 10f,
 *         far = 100f
 *     )
 * }
 *
 * // Add objects to scene
 * scene.add(mesh, light, camera)
 *
 * // Render the scene
 * renderer.render(scene, camera)
 * ```
 *
 * ## Scene Builder DSL
 *
 * KreeKt provides a declarative DSL for scene construction:
 *
 * ```kotlin
 * val scene = scene {
 *     // Set background
 *     background(Color(0x000000))
 *
 *     // Add fog
 *     fog(Color(0xFFFFFF), near = 1f, far = 100f)
 *
 *     // Create hierarchies
 *     group("buildings") {
 *         position(0f, 0f, 0f)
 *         add(building1, building2)
 *     }
 * }
 * ```
 *
 * ## Background Options
 *
 * Scenes support three types of backgrounds:
 * 1. **Solid Color**: `Background.Color(color)`
 * 2. **Skybox/Cubemap**: `Background.Texture(cubeTexture)`
 * 3. **Gradient**: `Background.Gradient(topColor, bottomColor)`
 *
 * ## Fog Types
 *
 * Two fog implementations are available:
 * - **Linear**: Fog density increases linearly between near and far distances
 * - **Exponential**: Fog density increases exponentially with distance
 *
 * ## Environment Lighting
 *
 * Set an environment map for realistic image-based lighting:
 * ```kotlin
 * scene.environment = loadCubeTexture("environment.hdr")
 * ```
 *
 * ## Performance Considerations
 *
 * - Scene itself has no transform (fixed at origin)
 * - Set [autoUpdate] to false for static scenes to skip unnecessary updates
 * - Use layers to control which objects are rendered
 * - Organize objects hierarchically for efficient culling
 *
 * @property background Visual background for the scene (null for transparent)
 * @property environment Environment map for IBL and reflections (null for no environment)
 * @property fog Atmospheric fog effect (null for no fog)
 * @property overrideMaterial Material to use for all objects (null for default materials)
 * @property autoUpdate Whether to automatically update scene state each frame (default: true)
 *
 * @see Object3D Parent class providing hierarchy and transformation
 * @see Background Scene background options
 * @see Fog Atmospheric fog types
 * @see scene DSL function for declarative scene construction
 *
 * @since 1.0.0
 * @sample io.kreekt.samples.SceneSamples.basicScene
 * @sample io.kreekt.samples.SceneSamples.sceneWithEnvironment
 */
class Scene : Object3D() {

    /**
     * Scene background - can be a solid color, cube texture (skybox), or gradient.
     *
     * Set to null for a transparent background (useful for AR or compositing).
     *
     * @see Background
     * @since 1.0.0
     */
    var background: Background? = null

    /**
     * Environment cubemap for image-based lighting and reflections.
     *
     * When set, materials with environment mapping will use this texture
     * for realistic lighting and reflections.
     *
     * @see CubeTexture
     * @since 1.0.0
     */
    var environment: CubeTexture? = null

    /**
     * Fog effect applied to all objects in the scene based on distance.
     *
     * Objects farther from the camera are progressively tinted with the fog color,
     * creating atmospheric depth and limiting visible range.
     *
     * @see Fog
     * @since 1.0.0
     */
    var fog: Fog? = null

    /**
     * Material that overrides all object materials during rendering.
     *
     * Useful for special rendering passes like depth rendering, normals, or
     * shadow map generation. Set to null to use each object's own material.
     *
     * @see Material
     * @since 1.0.0
     */
    var overrideMaterial: Material? = null

    /**
     * Whether scene properties are automatically updated each frame.
     *
     * Set to false for completely static scenes to improve performance.
     *
     * @since 1.0.0
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