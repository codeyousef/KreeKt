package io.kreekt.renderer.webgpu.shaders

/**
 * WGSL shader library for WebGPU renderer.
 * Contains vertex and fragment shaders for basic rendering.
 */
object BasicShaders {
    /**
     * Basic vertex shader with MVP transformation and per-vertex colors.
     *
     * Inputs:
     * - @location(0) position: vec3<f32> - Vertex position
     * - @location(1) normal: vec3<f32> - Vertex normal
     * - @location(2) color: vec3<f32> - Vertex color
     *
     * Uniforms:
     * - @group(0) @binding(0) projectionMatrix: mat4x4<f32>
     * - @group(0) @binding(1) viewMatrix: mat4x4<f32>
     * - @group(0) @binding(2) modelMatrix: mat4x4<f32>
     *
     * Outputs:
     * - @builtin(position) position: vec4<f32> - Clip space position
     * - @location(0) color: vec3<f32> - Interpolated color
     */
    val vertexShader = """
        struct Uniforms {
            projectionMatrix: mat4x4<f32>,
            viewMatrix: mat4x4<f32>,
            modelMatrix: mat4x4<f32>,
        };

        @group(0) @binding(0) var<uniform> uniforms: Uniforms;

        struct VertexInput {
            @location(0) position: vec3<f32>,
            @location(1) normal: vec3<f32>,
            @location(2) color: vec3<f32>,
        };

        struct VertexOutput {
            @builtin(position) position: vec4<f32>,
            @location(0) color: vec3<f32>,
        };

        @vertex
        fn vs_main(in: VertexInput) -> VertexOutput {
            var out: VertexOutput;
            let worldPosition = uniforms.modelMatrix * vec4<f32>(in.position, 1.0);
            let viewPosition = uniforms.viewMatrix * worldPosition;
            out.position = uniforms.projectionMatrix * viewPosition;
            out.color = in.color;
            return out;
        }
    """.trimIndent()

    /**
     * Basic fragment shader with per-vertex color interpolation.
     *
     * Inputs:
     * - @location(0) color: vec3<f32> - Interpolated vertex color
     *
     * Outputs:
     * - @location(0) color: vec4<f32> - Final fragment color (RGBA)
     */
    val fragmentShader = """
        struct FragmentInput {
            @location(0) color: vec3<f32>,
        };

        @fragment
        fn fs_main(in: FragmentInput) -> @location(0) vec4<f32> {
            // T021: Override to solid red for debugging
            return vec4<f32>(1.0, 0.0, 0.0, 1.0);
        }
    """.trimIndent()

    /**
     * Flat shading vertex shader (no interpolation, uses face normals).
     * Useful for low-poly aesthetic or debugging geometry.
     */
    val flatShadingVertexShader = """
        struct Uniforms {
            projectionMatrix: mat4x4<f32>,
            viewMatrix: mat4x4<f32>,
            modelMatrix: mat4x4<f32>,
        };

        @group(0) @binding(0) var<uniform> uniforms: Uniforms;

        struct VertexInput {
            @location(0) position: vec3<f32>,
            @location(1) normal: vec3<f32>,
            @location(2) color: vec3<f32>,
        };

        struct VertexOutput {
            @builtin(position) position: vec4<f32>,
            @location(0) @interpolate(flat) color: vec3<f32>,
        };

        @vertex
        fn vs_main(in: VertexInput) -> VertexOutput {
            var out: VertexOutput;
            let worldPosition = uniforms.modelMatrix * vec4<f32>(in.position, 1.0);
            let viewPosition = uniforms.viewMatrix * worldPosition;
            out.position = uniforms.projectionMatrix * viewPosition;
            out.color = in.color;
            return out;
        }
    """.trimIndent()

    /**
     * Wireframe fragment shader (renders edges only).
     * Useful for debugging geometry topology.
     */
    val wireframeFragmentShader = """
        struct FragmentInput {
            @location(0) color: vec3<f32>,
        };

        @fragment
        fn fs_main(in: FragmentInput) -> @location(0) vec4<f32> {
            return vec4<f32>(1.0, 1.0, 1.0, 1.0); // White wireframe
        }
    """.trimIndent()
}
