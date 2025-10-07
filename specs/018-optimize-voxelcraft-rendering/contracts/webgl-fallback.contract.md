# WebGL Fallback Contract

**Feature**: 018-optimize-voxelcraft-rendering
**Functional Requirements**: FR-003, FR-010, FR-011

## Contract Interface

```kotlin
interface WebGLFallbackContract {
    /**
     * FR-003: Minimum performance requirement
     * Renders scene using WebGL 2.0 fallback with constitutional minimum performance.
     * MUST achieve >= 30 FPS with 81 chunks (constitutional requirement).
     * @param scene The scene graph to render
     * @param camera The camera for view/projection matrices
     * @return RendererResult.Success or RendererResult.Error
     */
    fun render(scene: Scene, camera: Camera): RendererResult<Unit>

    /**
     * FR-010: Graceful fallback
     * Handles WebGPU initialization failure and falls back to WebGL.
     * MUST preserve all game state and provide seamless user experience.
     * @return RendererResult.Success or RendererResult.Error
     */
    fun fallbackToWebGL(): RendererResult<Unit>

    /**
     * FR-011: Visual consistency validation
     * Validates that WebGL output matches WebGPU reference rendering.
     * Used for regression testing to ensure consistent visual output.
     * @param webgpuOutput Reference image data from WebGPU renderer
     * @param webglOutput Output image data from WebGL renderer
     * @return true if outputs match within acceptable threshold (<1% pixel difference)
     */
    fun validateVisualConsistency(webgpuOutput: ImageData, webglOutput: ImageData): Boolean
}
```

## Contract Test Requirements

### Performance Tests (FR-003)
- Force WebGL fallback (disable WebGPU detection)
- Load 81 chunks with ~160K triangles
- Measure FPS over 2+ seconds
- Assert FPS >= 30 (constitutional minimum)
- Validate VAO usage (check gl.bindVertexArray calls)

### Fallback Tests (FR-010)
- Simulate WebGPU initialization failure
- Assert WebGL renderer initializes successfully
- Verify game state preserved (player position, world data)
- Check no errors logged during fallback

### Visual Consistency Tests (FR-011)
- Render same scene with WebGPU and WebGL
- Capture canvas ImageData from both
- Compare pixels (allow <1% difference for rounding errors)
- Validate lighting, colors, geometry match

## Acceptance Criteria

**FR-003**: WebGL renderer achieves 30+ FPS with 81 chunks (typically 35-40 FPS)
**FR-010**: WebGPU failure triggers WebGL fallback without crash or data loss
**FR-011**: Visual output matches WebGPU within <1% pixel difference
