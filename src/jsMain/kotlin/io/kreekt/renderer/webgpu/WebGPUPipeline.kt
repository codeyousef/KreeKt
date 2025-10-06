package io.kreekt.renderer.webgpu

import io.kreekt.renderer.RendererException
import io.kreekt.renderer.RendererResult

/**
 * WebGPU render pipeline implementation.
 * T032: Pipeline state management with shaders, vertex layout, depth/stencil, culling.
 */
class WebGPUPipeline(
    private val device: GPUDevice,
    private val descriptor: RenderPipelineDescriptor
) {
    private var pipeline: GPURenderPipeline? = null
    private var vertexShaderModule: WebGPUShaderModule? = null
    private var fragmentShaderModule: WebGPUShaderModule? = null

    /**
     * Creates the render pipeline.
     */
    suspend fun create(): RendererResult<Unit> {
        return try {
            // Compile shaders first
            vertexShaderModule = WebGPUShaderModule(
                device,
                ShaderModuleDescriptor(
                    label = "${descriptor.label ?: "pipeline"}_vertex",
                    code = descriptor.vertexShader,
                    stage = ShaderStage.VERTEX
                )
            )
            val vertexResult = vertexShaderModule!!.compile()
            if (vertexResult is RendererResult.Error) {
                return vertexResult
            }

            fragmentShaderModule = WebGPUShaderModule(
                device,
                ShaderModuleDescriptor(
                    label = "${descriptor.label ?: "pipeline"}_fragment",
                    code = descriptor.fragmentShader,
                    stage = ShaderStage.FRAGMENT
                )
            )
            val fragmentResult = fragmentShaderModule!!.compile()
            if (fragmentResult is RendererResult.Error) {
                return fragmentResult
            }

            // Create pipeline descriptor
            val pipelineDescriptor = js("({})").unsafeCast<GPURenderPipelineDescriptor>()
            descriptor.label?.let { pipelineDescriptor.label = it }
            pipelineDescriptor.layout = "auto"

            // Vertex state
            val vertexState = js("({})").unsafeCast<GPUVertexState>()
            vertexState.module = vertexShaderModule!!.getModule()!!
            vertexState.entryPoint = "vs_main"

            // Vertex buffer layout
            val bufferLayouts = js("[]").unsafeCast<Array<GPUVertexBufferLayout?>>()
            val bufferLayout = js("({})").unsafeCast<GPUVertexBufferLayout>()
            bufferLayout.arrayStride = descriptor.vertexBufferLayout.arrayStride
            bufferLayout.stepMode = when (descriptor.vertexBufferLayout.stepMode) {
                VertexStepMode.VERTEX -> "vertex"
                VertexStepMode.INSTANCE -> "instance"
            }

            // Vertex attributes
            val attributes = js("[]").unsafeCast<Array<GPUVertexAttribute>>()
            descriptor.vertexBufferLayout.attributes.forEach { attr ->
                val gpuAttr = js("({})").unsafeCast<GPUVertexAttribute>()
                gpuAttr.format = when (attr.format) {
                    VertexFormat.FLOAT32 -> "float32"
                    VertexFormat.FLOAT32X2 -> "float32x2"
                    VertexFormat.FLOAT32X3 -> "float32x3"
                    VertexFormat.FLOAT32X4 -> "float32x4"
                    VertexFormat.UINT32 -> "uint32"
                    VertexFormat.UINT32X2 -> "uint32x2"
                    VertexFormat.UINT32X3 -> "uint32x3"
                    VertexFormat.UINT32X4 -> "uint32x4"
                }
                gpuAttr.offset = attr.offset
                gpuAttr.shaderLocation = attr.shaderLocation
                js("attributes.push(gpuAttr)")
            }
            bufferLayout.attributes = attributes
            js("bufferLayouts.push(bufferLayout)")
            vertexState.buffers = bufferLayouts

            pipelineDescriptor.vertex = vertexState

            // Primitive state
            val primitiveState = js("({})").unsafeCast<GPUPrimitiveState>()
            primitiveState.topology = when (descriptor.primitiveTopology) {
                PrimitiveTopology.POINT_LIST -> "point-list"
                PrimitiveTopology.LINE_LIST -> "line-list"
                PrimitiveTopology.LINE_STRIP -> "line-strip"
                PrimitiveTopology.TRIANGLE_LIST -> "triangle-list"
                PrimitiveTopology.TRIANGLE_STRIP -> "triangle-strip"
            }
            primitiveState.cullMode = when (descriptor.cullMode) {
                CullMode.NONE -> "none"
                CullMode.FRONT -> "front"
                CullMode.BACK -> "back"
            }
            primitiveState.frontFace = when (descriptor.frontFace) {
                FrontFace.CCW -> "ccw"
                FrontFace.CW -> "cw"
            }
            pipelineDescriptor.primitive = primitiveState

            // Depth/stencil state (optional)
            descriptor.depthStencilState?.let { depthStencil ->
                val depthStencilState = js("({})").unsafeCast<GPUDepthStencilState>()
                depthStencilState.format = when (depthStencil.format) {
                    TextureFormat.DEPTH24_PLUS -> "depth24plus"
                    TextureFormat.DEPTH32_FLOAT -> "depth32float"
                    else -> "depth24plus"
                }
                depthStencilState.depthWriteEnabled = depthStencil.depthWriteEnabled
                depthStencilState.depthCompare = when (depthStencil.depthCompare) {
                    CompareFunction.NEVER -> "never"
                    CompareFunction.LESS -> "less"
                    CompareFunction.EQUAL -> "equal"
                    CompareFunction.LESS_EQUAL -> "less-equal"
                    CompareFunction.GREATER -> "greater"
                    CompareFunction.NOT_EQUAL -> "not-equal"
                    CompareFunction.GREATER_EQUAL -> "greater-equal"
                    CompareFunction.ALWAYS -> "always"
                }
                pipelineDescriptor.depthStencil = depthStencilState
            }

            // Fragment state
            val fragmentState = js("({})").unsafeCast<GPUFragmentState>()
            fragmentState.module = fragmentShaderModule!!.getModule()!!
            fragmentState.entryPoint = "fs_main"

            // Color target
            val colorTargets = js("[]").unsafeCast<Array<GPUColorTargetState?>>()
            val colorTarget = js("({})").unsafeCast<GPUColorTargetState>()
            colorTarget.format = "bgra8unorm"
            js("colorTargets.push(colorTarget)")
            fragmentState.targets = colorTargets

            pipelineDescriptor.fragment = fragmentState

            // Multisample state (optional)
            descriptor.multisampleState?.let { multisample ->
                val multisampleState = js("({})").unsafeCast<GPUMultisampleState>()
                multisampleState.count = multisample.count
                multisampleState.mask = multisample.mask
                multisampleState.alphaToCoverageEnabled = multisample.alphaToCoverageEnabled
                pipelineDescriptor.multisample = multisampleState
            }

            // Create the pipeline
            pipeline = device.createRenderPipeline(pipelineDescriptor)

            RendererResult.Success(Unit)
        } catch (e: Exception) {
            RendererResult.Error(RendererException.ResourceCreationFailed("Pipeline creation failed", e))
        }
    }

    /**
     * Gets the GPU render pipeline.
     */
    fun getPipeline(): GPURenderPipeline? = pipeline

    /**
     * Binds this pipeline to a render pass.
     */
    fun bind(renderPass: GPURenderPassEncoder) {
        pipeline?.let {
            renderPass.setPipeline(it)
        }
    }

    /**
     * Disposes the pipeline and shader modules.
     */
    fun dispose() {
        vertexShaderModule?.dispose()
        fragmentShaderModule?.dispose()
        pipeline = null
    }
}
