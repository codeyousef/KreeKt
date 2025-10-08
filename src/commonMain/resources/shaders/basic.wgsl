/**
 * T031: Basic WGSL Shader
 * Feature: 019-we-should-not
 *
 * Simple vertex and fragment shader for basic 3D rendering.
 * Compatible with WebGPU (native) and Vulkan (via SPIR-V compilation).
 */

// Uniform buffer for transformation matrices
struct Uniforms {
    modelViewProjection: mat4x4<f32>,
};

@group(0) @binding(0)
var<uniform> uniforms: Uniforms;

// Vertex shader input
struct VertexInput {
    @location(0) position: vec3<f32>,
    @location(1) color: vec3<f32>,
};

// Vertex shader output / Fragment shader input
struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec3<f32>,
};

// Vertex shader
@vertex
fn vs_main(input: VertexInput) -> VertexOutput {
    var output: VertexOutput;

    // Transform vertex position by MVP matrix
    output.position = uniforms.modelViewProjection * vec4<f32>(input.position, 1.0);

    // Pass color to fragment shader
    output.color = input.color;

    return output;
}

// Fragment shader
@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4<f32> {
    // Output per-vertex color with full opacity
    return vec4<f32>(input.color, 1.0);
}
