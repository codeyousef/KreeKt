/**
 * Scene Management API Contract
 * Defines interfaces for 3D scene creation and manipulation
 */
package kreekt.scene

import kreekt.core.*
import kreekt.geometry.*
import kreekt.material.*

/**
 * Scene builder DSL for creating 3D scenes
 */
fun scene(block: SceneBuilder.() -> Unit): Scene {
    return SceneBuilder().apply(block).build()
}

/**
 * DSL builder for constructing scenes
 */
class SceneBuilder {
    private val scene = Scene()

    /**
     * Add a mesh to the scene
     */
    fun mesh(
        geometry: BufferGeometry? = null,
        material: Material? = null,
        block: MeshBuilder.() -> Unit = {}
    ): Mesh {
        val mesh = MeshBuilder(geometry, material).apply(block).build()
        scene.add(mesh)
        return mesh
    }

    /**
     * Add a light to the scene
     */
    fun directionalLight(block: DirectionalLightBuilder.() -> Unit): DirectionalLight {
        val light = DirectionalLightBuilder().apply(block).build()
        scene.add(light)
        return light
    }

    fun pointLight(block: PointLightBuilder.() -> Unit): PointLight {
        val light = PointLightBuilder().apply(block).build()
        scene.add(light)
        return light
    }

    fun spotLight(block: SpotLightBuilder.() -> Unit): SpotLight {
        val light = SpotLightBuilder().apply(block).build()
        scene.add(light)
        return light
    }

    /**
     * Add a camera to the scene
     */
    fun perspectiveCamera(block: PerspectiveCameraBuilder.() -> Unit): PerspectiveCamera {
        val camera = PerspectiveCameraBuilder().apply(block).build()
        scene.add(camera)
        return camera
    }

    /**
     * Add a group for hierarchical organization
     */
    fun group(block: GroupBuilder.() -> Unit): Group {
        val group = GroupBuilder().apply(block).build()
        scene.add(group)
        return group
    }

    /**
     * Set scene background
     */
    fun background(color: Color) {
        scene.background = Background.Color(color)
    }

    fun background(texture: CubeTexture) {
        scene.background = Background.Texture(texture)
    }

    /**
     * Set scene fog
     */
    fun fog(color: Color, near: Float, far: Float) {
        scene.fog = Fog.Linear(color, near, far)
    }

    fun fogExp2(color: Color, density: Float) {
        scene.fog = Fog.Exponential(color, density)
    }

    internal fun build(): Scene = scene
}

/**
 * Mesh builder DSL
 */
class MeshBuilder(
    private var geometry: BufferGeometry? = null,
    private var material: Material? = null
) {
    private val mesh = Mesh(geometry, material)

    var position: Vector3
        get() = mesh.position
        set(value) { mesh.position.copy(value) }

    var rotation: Euler
        get() = mesh.rotation
        set(value) { mesh.rotation.copy(value) }

    var scale: Vector3
        get() = mesh.scale
        set(value) { mesh.scale.copy(value) }

    var visible: Boolean
        get() = mesh.visible
        set(value) { mesh.visible = value }

    var castShadow: Boolean
        get() = mesh.castShadow
        set(value) { mesh.castShadow = value }

    var receiveShadow: Boolean
        get() = mesh.receiveShadow
        set(value) { mesh.receiveShadow = value }

    fun geometry(geometry: BufferGeometry) {
        this.geometry = geometry
        mesh.geometry = geometry
    }

    fun material(material: Material) {
        this.material = material
        mesh.material = material
    }

    /**
     * Create geometry inline
     */
    fun boxGeometry(
        width: Float = 1f,
        height: Float = 1f,
        depth: Float = 1f,
        widthSegments: Int = 1,
        heightSegments: Int = 1,
        depthSegments: Int = 1
    ) {
        geometry(BoxGeometry(width, height, depth, widthSegments, heightSegments, depthSegments))
    }

    fun sphereGeometry(
        radius: Float = 1f,
        widthSegments: Int = 32,
        heightSegments: Int = 16,
        phiStart: Float = 0f,
        phiLength: Float = Math.PI.toFloat() * 2,
        thetaStart: Float = 0f,
        thetaLength: Float = Math.PI.toFloat()
    ) {
        geometry(SphereGeometry(radius, widthSegments, heightSegments, phiStart, phiLength, thetaStart, thetaLength))
    }

    fun planeGeometry(
        width: Float = 1f,
        height: Float = 1f,
        widthSegments: Int = 1,
        heightSegments: Int = 1
    ) {
        geometry(PlaneGeometry(width, height, widthSegments, heightSegments))
    }

    /**
     * Create material inline
     */
    fun standardMaterial(block: StandardMaterialBuilder.() -> Unit) {
        material(StandardMaterialBuilder().apply(block).build())
    }

    fun basicMaterial(block: BasicMaterialBuilder.() -> Unit) {
        material(BasicMaterialBuilder().apply(block).build())
    }

    internal fun build(): Mesh = mesh
}

/**
 * Light builder DSLs
 */
class DirectionalLightBuilder {
    private var color: Color = Color.WHITE
    private var intensity: Float = 1f
    private var castShadow: Boolean = false
    private val light = DirectionalLight(color, intensity)

    fun color(color: Color) {
        this.color = color
        light.color = color
    }

    fun intensity(intensity: Float) {
        this.intensity = intensity
        light.intensity = intensity
    }

    fun castShadow(enabled: Boolean) {
        this.castShadow = enabled
        light.castShadow = enabled
    }

    fun position(x: Float, y: Float, z: Float) {
        light.position.set(x, y, z)
    }

    fun lookAt(target: Vector3) {
        light.lookAt(target)
    }

    internal fun build(): DirectionalLight = light
}

/**
 * Asset loading interface
 */
interface AssetLoader {
    /**
     * Load a 3D model file
     */
    suspend fun loadModel(url: String): LoadResult<Group>

    /**
     * Load a texture image
     */
    suspend fun loadTexture(url: String): LoadResult<Texture>

    /**
     * Load an animation clip
     */
    suspend fun loadAnimation(url: String): LoadResult<AnimationClip>

    /**
     * Load multiple assets with progress tracking
     */
    fun loadAssets(
        urls: List<String>,
        onProgress: (loaded: Int, total: Int) -> Unit = { _, _ -> }
    ): Flow<LoadResult<Map<String, Any>>>
}

/**
 * Asset loading result
 */
sealed class LoadResult<T> {
    data class Success<T>(val value: T) : LoadResult<T>()
    data class Error<T>(val exception: LoadException) : LoadResult<T>()
    data class Progress<T>(val loaded: Long, val total: Long) : LoadResult<T>()
}

/**
 * Asset loading exceptions
 */
sealed class LoadException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class FileNotFound(url: String) : LoadException("Asset not found: $url")
    class UnsupportedFormat(format: String) : LoadException("Unsupported format: $format")
    class ParseError(message: String, cause: Throwable? = null) : LoadException("Parse error: $message", cause)
    class NetworkError(message: String, cause: Throwable? = null) : LoadException("Network error: $message", cause)
}

/**
 * Raycasting interface for object selection
 */
interface Raycaster {
    var ray: Ray
    var near: Float
    var far: Float
    val params: RaycastParams

    fun setFromCamera(coords: Vector2, camera: Camera)
    fun intersectObject(object3d: Object3D, recursive: Boolean = false): List<Intersection>
    fun intersectObjects(objects: List<Object3D>, recursive: Boolean = false): List<Intersection>
}

data class RaycastParams(
    var meshThreshold: Float = 1f,
    var lineThreshold: Float = 1f,
    var pointThreshold: Float = 1f
)

/**
 * Event system for user interaction
 */
interface InteractionManager {
    fun addEventListener(type: String, listener: (Event) -> Unit)
    fun removeEventListener(type: String, listener: (Event) -> Unit)
    fun dispatchEvent(event: Event)
}

sealed class Event {
    data class PointerEvent(
        val type: String,
        val position: Vector2,
        val button: Int,
        val object3d: Object3D?
    ) : Event()

    data class KeyboardEvent(
        val type: String,
        val key: String,
        val code: String,
        val shiftKey: Boolean,
        val ctrlKey: Boolean,
        val altKey: Boolean
    ) : Event()
}