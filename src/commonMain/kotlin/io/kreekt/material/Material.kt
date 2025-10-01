package io.kreekt.material

/**
 * Base material class implementing the core Material interface
 */
abstract class Material : io.kreekt.core.scene.Material {

    override val id: Int = nextId++
    override var name: String = ""
    override var needsUpdate: Boolean = true
    override var visible: Boolean = true

    // Common material properties
    open var opacity: Float = 1f
    open var transparent: Boolean = false
    open var vertexColors: Boolean = false
    open var depthWrite: Boolean = true
    open var depthTest: Boolean = true
    open var side: Int = FRONT_SIDE

    abstract val type: String

    /**
     * Clone this material (must be implemented by subclasses)
     */
    abstract fun clone(): Material

    /**
     * Copy common properties from another material
     */
    open fun copy(source: Material): Material {
        this.name = source.name
        this.needsUpdate = source.needsUpdate
        this.visible = source.visible
        this.opacity = source.opacity
        this.transparent = source.transparent
        this.vertexColors = source.vertexColors
        this.depthWrite = source.depthWrite
        this.depthTest = source.depthTest
        this.side = source.side
        return this
    }

    /**
     * Dispose of this material
     */
    open fun dispose() {
        // Override in subclasses to clean up resources
    }

    companion object {
        private var nextId = 1

        // Side constants
        const val FRONT_SIDE = 0
        const val BACK_SIDE = 1
        const val DOUBLE_SIDE = 2
    }
}