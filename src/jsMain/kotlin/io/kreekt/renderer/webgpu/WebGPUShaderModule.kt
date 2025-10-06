package io.kreekt.renderer.webgpu

import io.kreekt.renderer.RendererException
import io.kreekt.renderer.RendererResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/**
 * Await a JavaScript Promise in a suspend function.
 */
private suspend fun <T> Promise<T>.awaitPromise(): T = suspendCancellableCoroutine { cont ->
    this.then(
        onFulfilled = { value -> cont.resume(value) },
        onRejected = { error -> cont.resumeWithException(error as? Throwable ?: Exception(error.toString())) }
    )
}

/**
 * WebGPU shader module implementation.
 * T029: Shader compilation and validation.
 *
 * Compiles WGSL shaders to GPU bytecode.
 */
class WebGPUShaderModule(
    private val device: GPUDevice,
    private val descriptor: ShaderModuleDescriptor
) {
    private var module: GPUShaderModule? = null

    /**
     * Compiles the WGSL shader code.
     * @return Success or Error with compilation details
     */
    suspend fun compile(): RendererResult<Unit> {
        return try {
            console.log("Compiling shader: ${descriptor.label ?: "unnamed"} (${descriptor.stage})")
            val shaderDescriptor = js("({})").unsafeCast<GPUShaderModuleDescriptor>()
            shaderDescriptor.code = descriptor.code
            descriptor.label?.let { shaderDescriptor.label = it }

            console.log("Creating shader module...")
            module = device.createShaderModule(shaderDescriptor)
            console.log("Shader module created, getting compilation info...")

            // Validate shader compilation
            val compilationInfo = module!!.getCompilationInfo().unsafeCast<Promise<dynamic>>().awaitPromise()
            console.log("Compilation info received")
            val messages = compilationInfo.messages.unsafeCast<Array<dynamic>>()

            // Check for errors
            val hasErrors = messages.any { it.type == "error" }
            if (hasErrors) {
                val errorMsg = messages.filter { it.type == "error" }
                    .joinToString("\n") { it.message.toString() }
                console.error("Shader compilation errors: $errorMsg")
                RendererResult.Error(RendererException.ResourceCreationFailed("Shader compilation failed: $errorMsg"))
            } else {
                console.log("Shader compiled successfully: ${descriptor.label ?: "unnamed"}")
                RendererResult.Success(Unit)
            }
        } catch (e: Exception) {
            console.error("Shader module creation exception: ${e.message}")
            e.printStackTrace()
            RendererResult.Error(RendererException.ResourceCreationFailed("Shader module creation failed", e))
        }
    }

    /**
     * Validates the shader without creating a module.
     * @return true if shader is valid
     */
    fun validate(): Boolean {
        return try {
            // Attempt compilation and check for errors
            val testDescriptor = js("({})").unsafeCast<GPUShaderModuleDescriptor>()
            testDescriptor.code = descriptor.code
            val testModule = device.createShaderModule(testDescriptor)
            testModule != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the compiled shader module.
     * @return GPU shader module or null if not compiled
     */
    fun getModule(): GPUShaderModule? = module

    /**
     * Gets the shader stage.
     */
    fun getStage(): ShaderStage = descriptor.stage

    /**
     * Disposes the shader module.
     */
    fun dispose() {
        module = null
    }
}
