/**
 * Controls API Contract
 *
 * This file defines the complete API surface for the camera controls subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.controls

import io.kreekt.core.math.*
import io.kreekt.controls.*
import io.kreekt.camera.*
import io.kreekt.scene.*

// ============================================================================
// Core Controls API
// ============================================================================

/**
 * OrbitControls: Rotate camera around target point
 * Three.js equivalent: THREE.OrbitControls
 */
interface OrbitControlsAPI {
    val camera: Camera
    val domElement: Any?
    var enabled: Boolean

    // Target of orbit
    val target: Vector3

    // Movement speeds
    var rotateSpeed: Float
    var zoomSpeed: Float
    var panSpeed: Float

    // Constraints
    var minDistance: Float
    var maxDistance: Float
    var minZoom: Float
    var maxZoom: Float
    var minPolarAngle: Float  // Radians
    var maxPolarAngle: Float  // Radians
    var minAzimuthAngle: Float  // Radians
    var maxAzimuthAngle: Float  // Radians

    // Enable/disable features
    var enableDamping: Boolean
    var dampingFactor: Float
    var enableZoom: Boolean
    var enableRotate: Boolean
    var enablePan: Boolean

    // Auto rotation
    var autoRotate: Boolean
    var autoRotateSpeed: Float

    // Screen space panning
    var screenSpacePanning: Boolean
    var keyPanSpeed: Float

    // Mouse buttons
    var mouseButtons: MouseButtons
    var touches: Touches

    // Keys
    var keys: Keys

    // State
    fun update(): Boolean
    fun saveState()
    fun reset()
    fun dispose()

    // Methods
    fun getPolarAngle(): Float
    fun getAzimuthalAngle(): Float
    fun getDistance(): Float
    fun listenToKeyEvents(domElement: Any)
    fun stopListenToKeyEvents()

    // Events
    fun addEventListener(type: String, listener: (Any) -> Unit)
    fun hasEventListener(type: String): Boolean
    fun removeEventListener(type: String, listener: (Any) -> Unit)
    fun dispatchEvent(event: Any)
}

data class MouseButtons(
    var LEFT: MouseButton = MouseButton.ROTATE,
    var MIDDLE: MouseButton = MouseButton.DOLLY,
    var RIGHT: MouseButton = MouseButton.PAN
)

data class Touches(
    var ONE: TouchAction = TouchAction.ROTATE,
    var TWO: TouchAction = TouchAction.DOLLY_PAN
)

data class Keys(
    var LEFT: String = "ArrowLeft",
    var UP: String = "ArrowUp",
    var RIGHT: String = "ArrowRight",
    var BOTTOM: String = "ArrowDown"
)

enum class MouseButton {
    ROTATE,
    DOLLY,
    PAN,
    NONE
}

enum class TouchAction {
    ROTATE,
    PAN,
    DOLLY_PAN,
    DOLLY_ROTATE,
    NONE
}

// ============================================================================
// First Person Controls
// ============================================================================

/**
 * FirstPersonControls: Classic FPS-style camera controls
 * Three.js equivalent: THREE.FirstPersonControls
 */
interface FirstPersonControlsAPI {
    val camera: Camera
    val domElement: Any?
    var enabled: Boolean

    var movementSpeed: Float
    var lookSpeed: Float
    var lookVertical: Boolean
    var autoForward: Boolean
    var activeLook: Boolean
    var heightSpeed: Boolean
    var heightCoef: Float
    var heightMin: Float
    var heightMax: Float
    var constrainVertical: Boolean
    var verticalMin: Float
    var verticalMax: Float
    var mouseDragOn: Boolean

    fun update(delta: Float)
    fun dispose()

    fun handleResize()
}

// ============================================================================
// Fly Controls
// ============================================================================

/**
 * FlyControls: 6DOF flight controls
 * Three.js equivalent: THREE.FlyControls
 */
interface FlyControlsAPI {
    val camera: Camera
    val domElement: Any?
    var enabled: Boolean

    var movementSpeed: Float
    var rollSpeed: Float
    var dragToLook: Boolean
    var autoForward: Boolean

    fun update(delta: Float)
    fun dispose()
}

// ============================================================================
// Trackball Controls
// ============================================================================

/**
 * TrackballControls: Trackball-style rotation
 * Three.js equivalent: THREE.TrackballControls
 */
interface TrackballControlsAPI {
    val camera: Camera
    val domElement: Any?
    var enabled: Boolean

    val screen: Screen
    var rotateSpeed: Float
    var zoomSpeed: Float
    var panSpeed: Float
    var noRotate: Boolean
    var noZoom: Boolean
    var noPan: Boolean
    var noRoll: Boolean
    var staticMoving: Boolean
    var dynamicDampingFactor: Float
    var minDistance: Float
    var maxDistance: Float

    var keys: IntArray  // Key codes for pan

    val target: Vector3

    fun update()
    fun reset()
    fun dispose()

    fun checkDistances()
    fun zoomCamera()
    fun panCamera()
    fun rotateCamera()

    fun handleResize()
}

data class Screen(
    var left: Int = 0,
    var top: Int = 0,
    var width: Int = 0,
    var height: Int = 0
)

// ============================================================================
// ArcballControls
// ============================================================================

/**
 * ArcballControls: Mathematical arcball rotation
 * Three.js equivalent: THREE.ArcballControls
 */
interface ArcballControlsAPI {
    val camera: Camera
    val domElement: Any?
    var enabled: Boolean

    val target: Vector3
    var cursorZoom: Boolean
    var adjustNearFar: Boolean
    var scaleFactor: Float
    var dampingFactor: Float
    var wMax: Float
    var enableAnimations: Boolean
    var enableGrid: Boolean
    var enableRotate: Boolean
    var enablePan: Boolean
    var enableZoom: Boolean
    var minDistance: Float
    var maxDistance: Float
    var minZoom: Float
    var maxZoom: Float
    var radiusFactor: Float

    fun update()
    fun setCamera(camera: Camera)
    fun setGizmosVisible(value: Boolean)
    fun setTbRadius(value: Float)
    fun dispose()
    fun reset()
    fun saveState()

    fun getRaycaster(): Raycaster
    fun activateGizmos(isActive: Boolean)
    fun copyState(): ArcballControlsState
    fun pasteState(state: ArcballControlsState)
}

data class ArcballControlsState(
    val camera: Camera,
    val gizmos: Any,
    val target: Vector3,
    val up: Vector3,
    val zoom: Float
)

// ============================================================================
// Pointer Lock Controls
// ============================================================================

/**
 * PointerLockControls: First-person with pointer lock
 * Three.js equivalent: THREE.PointerLockControls
 */
interface PointerLockControlsAPI {
    val camera: Camera
    val domElement: Any?
    var isLocked: Boolean

    val minPolarAngle: Float
    val maxPolarAngle: Float
    val pointerSpeed: Float

    fun connect()
    fun disconnect()
    fun dispose()
    fun getObject(): Object3D
    fun getDirection(v: Vector3): Vector3
    fun moveForward(distance: Float)
    fun moveRight(distance: Float)
    fun lock()
    fun unlock()

    fun addEventListener(type: String, listener: (Any) -> Unit)
    fun hasEventListener(type: String): Boolean
    fun removeEventListener(type: String, listener: (Any) -> Unit)
}

// ============================================================================
// Transform Controls
// ============================================================================

/**
 * TransformControls: Interactive object transformation gizmos
 * Three.js equivalent: THREE.TransformControls
 */
interface TransformControlsAPI : Object3DAPI {
    val camera: Camera
    val domElement: Any?
    var enabled: Boolean

    // Mode
    var mode: TransformMode
    var space: TransformSpace
    var axis: String?

    // Size and appearance
    var size: Float
    var showX: Boolean
    var showY: Boolean
    var showZ: Boolean

    // Behavior
    var translationSnap: Float?
    var rotationSnap: Float?
    var scaleSnap: Float?
    var dragging: Boolean

    // Methods
    fun attach(obj: Object3D): TransformControls
    fun detach(): TransformControls
    fun getMode(): TransformMode
    fun setMode(mode: TransformMode)
    fun setTranslationSnap(snap: Float?)
    fun setRotationSnap(snap: Float?)
    fun setScaleSnap(snap: Float?)
    fun setSize(size: Float)
    fun setSpace(space: TransformSpace)
    fun getRaycaster(): Raycaster
    fun dispose()
    fun reset()

    // Events: "change", "mouseDown", "mouseUp", "objectChange"
    fun addEventListener(type: String, listener: (Any) -> Unit)
    fun hasEventListener(type: String): Boolean
    fun removeEventListener(type: String, listener: (Any) -> Unit)
}

enum class TransformMode {
    Translate,
    Rotate,
    Scale
}

enum class TransformSpace {
    World,
    Local
}

// ============================================================================
// Drag Controls
// ============================================================================

/**
 * DragControls: Drag objects in 3D space
 * Three.js equivalent: THREE.DragControls
 */
interface DragControlsAPI {
    val objects: List<Object3D>
    val camera: Camera
    val domElement: Any?
    var enabled: Boolean

    var transformGroup: Boolean
    var mode: DragMode
    var rotateSpeed: Float
    var recursive: Boolean

    fun activate()
    fun deactivate()
    fun dispose()
    fun getObjects(): List<Object3D>
    fun getRaycaster(): Raycaster

    // Events: "dragstart", "drag", "dragend", "hoveron", "hoveroff"
    fun addEventListener(type: String, listener: (DragEvent) -> Unit)
    fun hasEventListener(type: String): Boolean
    fun removeEventListener(type: String, listener: (DragEvent) -> Unit)
}

enum class DragMode {
    Translate,
    Rotate
}

data class DragEvent(
    val type: String,
    val obj: Object3D?
)

// ============================================================================
// Map Controls
// ============================================================================

/**
 * MapControls: OrbitControls configured for top-down map view
 * Three.js equivalent: THREE.MapControls (extends OrbitControls)
 */
interface MapControlsAPI : OrbitControlsAPI {
    // Inherits all OrbitControls properties
    // Default configuration for top-down viewing
}

// ============================================================================
// Supporting Types
// ============================================================================

interface Raycaster {
    val ray: Ray
    var near: Float
    var far: Float
    val camera: Camera?
    val layers: Layers
    val params: RaycasterParameters

    fun set(origin: Vector3, direction: Vector3)
    fun setFromCamera(coords: Vector2, camera: Camera)
    fun intersectObject(obj: Object3D, recursive: Boolean = false): List<Intersection>
    fun intersectObjects(objects: List<Object3D>, recursive: Boolean = false): List<Intersection>
}

data class RaycasterParameters(
    var Mesh: MeshRaycasterParams = MeshRaycasterParams(),
    var Line: LineRaycasterParams = LineRaycasterParams(),
    var Points: PointsRaycasterParams = PointsRaycasterParams()
)

data class MeshRaycasterParams(
    var threshold: Float = 0f
)

data class LineRaycasterParams(
    var threshold: Float = 1f
)

data class PointsRaycasterParams(
    var threshold: Float = 1f
)

data class Intersection(
    val distance: Float,
    val point: Vector3,
    val obj: Object3D,
    val face: Face3? = null,
    val faceIndex: Int? = null,
    val uv: Vector2? = null,
    val uv2: Vector2? = null,
    val instanceId: Int? = null
)

data class Face3(
    val a: Int,
    val b: Int,
    val c: Int,
    val normal: Vector3,
    val materialIndex: Int = 0
)

interface Layers {
    var mask: Int
    fun set(channel: Int)
    fun enable(channel: Int)
    fun enableAll()
    fun toggle(channel: Int)
    fun disable(channel: Int)
    fun disableAll()
    fun test(layers: Layers): Boolean
    fun isEnabled(channel: Int): Boolean
}

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for OrbitControls
 */
fun orbitControls(
    camera: Camera,
    domElement: Any? = null,
    init: OrbitControls.() -> Unit = {}
): OrbitControls {
    val controls = OrbitControls(camera, domElement)
    controls.init()
    return controls
}

/**
 * DSL builder for TransformControls
 */
fun transformControls(
    camera: Camera,
    domElement: Any? = null,
    init: TransformControls.() -> Unit = {}
): TransformControls {
    val controls = TransformControls(camera, domElement)
    controls.init()
    return controls
}

/**
 * Extension function to configure for smooth orbit
 */
fun OrbitControls.enableSmoothOrbit() {
    enableDamping = true
    dampingFactor = 0.05f
    rotateSpeed = 0.5f
    zoomSpeed = 1.0f
    panSpeed = 0.8f
}

/**
 * Extension function to configure for map view
 */
fun OrbitControls.configureForMapView() {
    screenSpacePanning = false
    minDistance = 10f
    maxDistance = 500f
    maxPolarAngle = Math.PI.toFloat() / 2f
    enableDamping = true
    dampingFactor = 0.1f
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Basic orbit controls
 */
fun exampleOrbitControls(camera: Camera, domElement: Any): OrbitControls {
    return orbitControls(camera, domElement) {
        enableDamping = true
        dampingFactor = 0.05f
        rotateSpeed = 0.5f
        zoomSpeed = 1.0f
        panSpeed = 0.8f

        minDistance = 5f
        maxDistance = 100f
        maxPolarAngle = Math.PI.toFloat() / 2f

        target.set(0f, 0f, 0f)
    }
}

/**
 * Example: First person controls with movement
 */
fun exampleFirstPersonControls(
    camera: Camera,
    domElement: Any
): FirstPersonControls {
    return FirstPersonControls(camera, domElement).apply {
        movementSpeed = 10f
        lookSpeed = 0.1f
        lookVertical = true
        autoForward = false
        activeLook = true
    }
}

/**
 * Example: Transform controls for object editing
 */
fun exampleTransformControls(
    camera: Camera,
    renderer: Renderer,
    scene: Scene,
    obj: Object3D
): TransformControls {
    val controls = transformControls(camera, renderer.domElement) {
        attach(obj)
        mode = TransformMode.Translate
        space = TransformSpace.World
        size = 1f
        showX = true
        showY = true
        showZ = true
    }

    scene.add(controls)

    // Handle transform changes
    controls.addEventListener("dragging-changed") { event ->
        val dragging = event as? Boolean ?: false
        // Disable orbit controls while dragging
    }

    controls.addEventListener("change") { event ->
        // Object transformed, update render
    }

    return controls
}

/**
 * Example: Drag controls for interactive objects
 */
fun exampleDragControls(
    objects: List<Object3D>,
    camera: Camera,
    domElement: Any
): DragControls {
    val controls = DragControls(objects, camera, domElement).apply {
        enabled = true
        transformGroup = false
        mode = DragMode.Translate
    }

    controls.addEventListener("dragstart") { event ->
        println("Drag started: ${event.obj?.name}")
    }

    controls.addEventListener("drag") { event ->
        println("Dragging: ${event.obj?.position}")
    }

    controls.addEventListener("dragend") { event ->
        println("Drag ended: ${event.obj?.name}")
    }

    controls.addEventListener("hoveron") { event ->
        // Change cursor or highlight object
        event.obj?.let { obj ->
            // obj.material.emissive.setHex(0x555555)
        }
    }

    controls.addEventListener("hoveroff") { event ->
        // Reset visual state
        event.obj?.let { obj ->
            // obj.material.emissive.setHex(0x000000)
        }
    }

    return controls
}

/**
 * Example: Pointer lock controls for FPS game
 */
fun examplePointerLockControls(
    camera: Camera,
    domElement: Any,
    velocity: Vector3 = Vector3(),
    direction: Vector3 = Vector3()
): PointerLockControlsAPI {
    val controls = PointerLockControls(camera, domElement)

    // Movement handling (called in animation loop)
    fun updateMovement(delta: Float, moveForward: Boolean, moveBackward: Boolean, moveLeft: Boolean, moveRight: Boolean) {
        direction.z = if (moveForward) 1f else if (moveBackward) -1f else 0f
        direction.x = if (moveRight) 1f else if (moveLeft) -1f else 0f
        direction.normalize()

        if (moveForward || moveBackward) controls.moveForward(direction.z * delta * 100f)
        if (moveLeft || moveRight) controls.moveRight(direction.x * delta * 100f)
    }

    // Events
    controls.addEventListener("lock") {
        println("Pointer locked")
    }

    controls.addEventListener("unlock") {
        println("Pointer unlocked")
    }

    return controls
}

/**
 * Example: Map controls for top-down view
 */
fun exampleMapControls(camera: Camera, domElement: Any): MapControls {
    return MapControls(camera, domElement).apply {
        configureForMapView()
        enableRotate = false  // Optional: disable rotation for pure top-down
    }
}

/**
 * Example: Arcball controls with grid
 */
fun exampleArcballControls(camera: Camera, domElement: Any): ArcballControls {
    return ArcballControls(camera, domElement).apply {
        enableGrid = true
        enableAnimations = true
        dampingFactor = 0.25f
        scaleFactor = 1.1f
        minDistance = 1f
        maxDistance = 100f

        setGizmosVisible(true)
    }
}

/**
 * Example: Update controls in animation loop
 */
fun exampleAnimationLoop(
    orbitControls: OrbitControls,
    transformControls: TransformControls,
    clock: Clock
) {
    val delta = clock.getDelta()

    // Update orbit controls
    if (orbitControls.enabled) {
        orbitControls.update()
    }

    // Transform controls update automatically
}
