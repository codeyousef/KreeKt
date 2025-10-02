/**
 * Node-Based Material System Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Node-Based Materials (TSL Equivalent)
 *
 * Requirements covered: FR-051 through FR-056
 */

package io.kreekt.material.nodes

import io.kreekt.core.Result
import io.kreekt.core.math.Vector2
import io.kreekt.material.Material
import io.kreekt.material.Shader

/**
 * Base class for material graph nodes.
 *
 * FR-051: System MUST provide a node-based material composition system equivalent to Three.js Shading Language (TSL)
 */
abstract class MaterialNode {
    abstract val id: String
    abstract val name: String
    abstract val inputs: List<NodeInput>
    abstract val outputs: List<NodeOutput>

    var position: Vector2 = Vector2.zero()

    /**
     * Generates shader code for this node.
     *
     * FR-054: System MUST generate WGSL shader code from node graphs for WebGPU
     * FR-055: System MUST generate SPIR-V compatible code for Vulkan platforms
     */
    abstract fun generateCode(target: ShaderTarget, context: CodeGenContext): String

    abstract fun validate(): ValidationResult
    open fun clone(): MaterialNode = throw NotImplementedError()
}

/**
 * Node input socket.
 */
data class NodeInput(
    val name: String,
    val type: NodeDataType,
    val node: MaterialNode,
    var connection: NodeOutput? = null,
    var defaultValue: Any? = null
) {
    val isConnected: Boolean get() = connection != null
    fun getValue(): Any = connection?.getValue() ?: defaultValue ?: getTypeDefault(type)
}

/**
 * Node output socket.
 */
data class NodeOutput(
    val name: String,
    val type: NodeDataType,
    val node: MaterialNode,
    val connections: MutableList<NodeInput> = mutableListOf()
) {
    fun getValue(): Any = node.computeOutput(name)
    fun connect(input: NodeInput)
    fun disconnect(input: NodeInput)
}

/**
 * Type system for node data.
 */
enum class NodeDataType(val size: Int, val wgslType: String) {
    FLOAT(1, "f32"),
    VEC2(2, "vec2<f32>"),
    VEC3(3, "vec3<f32>"),
    VEC4(4, "vec4<f32>"),
    MAT3(9, "mat3x3<f32>"),
    MAT4(16, "mat4x4<f32>"),
    TEXTURE_2D(0, "texture_2d<f32>"),
    TEXTURE_CUBE(0, "texture_cube<f32>"),
    SAMPLER(0, "sampler"),
    BOOL(1, "bool"),
    INT(1, "i32");

    fun isCompatibleWith(other: NodeDataType): Boolean
}

/**
 * Container for material node graph.
 *
 * FR-052: System MUST support material nodes for common operations (math, texture sampling, lighting)
 */
class NodeGraph {
    var name: String = "NodeGraph"
    var outputNode: FragmentOutputNode? = null

    fun addNode(node: MaterialNode)
    fun removeNode(nodeId: String)
    fun connect(from: NodeOutput, to: NodeInput): Result<Unit>
    fun disconnect(from: NodeOutput, to: NodeInput)

    fun compile(target: ShaderTarget): Result<CompiledShader>
    fun validate(): ValidationResult
}

/**
 * Shader target platform.
 */
enum class ShaderTarget {
    WGSL,   // WebGPU
    SPIRV   // Vulkan
}

/**
 * Code generation context.
 */
class CodeGenContext(val target: ShaderTarget) {
    fun addNodeCode(nodeId: String, code: String)
    fun finalize(): Result<CompiledShader>
}

/**
 * Compiled shader result.
 */
data class CompiledShader(
    val vertexShader: String,
    val fragmentShader: String,
    val uniforms: Map<String, io.kreekt.material.Uniform>
)

/**
 * Validation result.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * Material that uses a node graph.
 *
 * FR-056: Node system MUST integrate with existing Material classes
 */
class NodeMaterial : Material() {
    val nodeGraph: NodeGraph
    var autoCompile: Boolean = true

    override fun onBeforeCompile(shader: Shader, renderer: io.kreekt.renderer.Renderer)
    fun recompile()
}

// Node types (FR-052, FR-053)

/**
 * Texture sampling node.
 */
class TextureSampleNode(
    val texture: io.kreekt.renderer.Texture
) : MaterialNode()

/**
 * Vertex attribute node.
 */
class AttributeNode(
    val attributeName: String
) : MaterialNode()

/**
 * Uniform value node.
 */
class UniformNode(
    val uniformName: String,
    val uniformType: NodeDataType
) : MaterialNode()

/**
 * Time node (for animations).
 */
class TimeNode : MaterialNode()

/**
 * Math operation nodes.
 */
class AddNode : MaterialNode()
class SubtractNode : MaterialNode()
class MultiplyNode : MaterialNode()
class DivideNode : MaterialNode()
class DotProductNode : MaterialNode()
class CrossProductNode : MaterialNode()
class NormalizeNode : MaterialNode()

/**
 * Lighting nodes.
 */
class PBRNode : MaterialNode()
class LambertNode : MaterialNode()
class PhongNode : MaterialNode()
class FresnelNode : MaterialNode()

/**
 * Utility nodes.
 */
class SplitNode : MaterialNode()
class CombineNode : MaterialNode()
class ConditionalNode : MaterialNode()
class FunctionNode(val code: String) : MaterialNode()

/**
 * Output nodes.
 */
class FragmentOutputNode : MaterialNode()
class VertexOutputNode : MaterialNode()
