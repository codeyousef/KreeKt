# Next Steps: VoxelCraft Black Screen Debugging (T021)

## Current Status
- **Problem**: Black screen in VoxelCraft despite successful rendering execution
- **Evidence**:
  - Rendering executes: 24+ draw calls, 40k+ triangles
  - Camera position: (0, 100, 0) ✓
  - Camera rotation: (-0.3, 0, 0) ✓
  - **View matrix is IDENTITY** `[1, 0, 0, 0]` ❌
- **Latest Build**: Port 8082

## Root Cause Investigation

### Confirmed Working
- ✅ WebGPU initialization and pipeline creation
- ✅ Mesh generation (81 chunks, ~94k triangles)
- ✅ Draw call execution (40+ calls)
- ✅ Camera position sync from player
- ✅ Camera rotation sync from player
- ✅ Quaternion updated from Euler (Main.kt:284)

### Root Cause
View matrix (`camera.matrixWorldInverse`) is identity matrix despite:
1. Camera at position (0, 100, 0)
2. Camera rotation (-0.3, 0, 0)
3. `camera.updateMatrixWorld(true)` called at Main.kt:297
4. `camera.quaternion.setFromEuler()` called at Main.kt:284

**Evidence from logs (port 8081)**:
```
T021 Projection matrix[0..3]: [0.977, 0, 0, 0]  ✓ Correct
T021 View matrix[0..3]: [1, 0, 0, 0]            ❌ IDENTITY - should reflect translation
T021 Model matrix[0..3]: [1, 0, 0, 0]           ✓ Correct
T021 Camera position: (0, 100, 0)               ✓ Correct
T021 Camera rotation: (-0.3, 0, 0)              ✓ Correct
```

## Investigation Steps to Continue

### 1. Verify Matrix Update Chain
Check if Camera.updateMatrixWorld() is actually computing matrixWorldInverse:
- Add diagnostic logging in Camera.kt:161 (updateMatrixWorld override)
- Log matrixWorld BEFORE and AFTER invert() call
- Verify invert() is actually being called

**File**: `src/commonMain/kotlin/io/kreekt/camera/Camera.kt`
**Line**: 159-162
```kotlin
override fun updateMatrixWorld(force: Boolean) {
    super.updateMatrixWorld(force)
    matrixWorldInverse.copy(matrixWorld).invert()  // ← Verify this executes
}
```

### 2. Check Matrix4.invert() Implementation
Verify Matrix4.invert() is correctly implemented:
- Read `src/commonMain/kotlin/io/kreekt/core/math/Matrix4.kt`
- Check invert() method implementation
- Ensure it's not returning early or failing silently

### 3. Verify Object3D.updateMatrixWorld()
Check if Object3D.updateMatrixWorld() is updating matrixWorld from position/quaternion:
- Add logging in Object3D.kt:256 (updateMatrixWorld)
- Log matrix BEFORE and AFTER compose() call
- Verify matrixWorld reflects position and quaternion

**File**: `src/commonMain/kotlin/io/kreekt/core/scene/Object3D.kt`
**Line**: 256-287

### 4. Alternative: Direct View Matrix Construction
If matrix update chain is broken, construct view matrix directly:
- In Main.kt game loop, after camera sync
- Use lookAt or manual matrix construction
- Example:
```kotlin
// Temporary workaround - construct view matrix directly
val viewMatrix = Matrix4()
viewMatrix.makeTranslation(-camera.position.x, -camera.position.y, -camera.position.z)
viewMatrix.multiply(Matrix4().makeRotationFromQuaternion(camera.quaternion.clone().invert()))
camera.matrixWorldInverse.copy(viewMatrix)
```

### 5. Check Euler onChange Callbacks
Despite adding manual quaternion sync, verify onChange is the real issue:
- Add onChange callback implementation to Euler.kt
- Or verify all rotation setting goes through proper sync

## Files Modified So Far

### Main.kt (examples/voxelcraft/src/jsMain/kotlin/io/kreekt/examples/voxelcraft/)
- Line 76: Reset rotation to (-0.3, 0, 0) for spawn
- Line 277-287: Camera rotation sync with manual quaternion update
- Line 291-293: Diagnostic logging

### Player.kt (examples/voxelcraft/src/commonMain/kotlin/io/kreekt/examples/voxelcraft/)
- Line 23: Initial rotation set to (-0.3, 0, 0)

### WebGPURenderer.kt (src/jsMain/kotlin/io/kreekt/renderer/webgpu/)
- Line 405: Disabled backface culling (diagnostic)
- Line 467-476: Vertex color logging (diagnostic)
- Line 582-620: MVP matrix logging (diagnostic)

### BasicShaders.kt (src/jsMain/kotlin/io/kreekt/renderer/webgpu/shaders/)
- Line 72: Fragment shader override to solid red (diagnostic)

## Diagnostic Changes to Revert After Fix
1. Restore fragment shader to use vertex colors (BasicShaders.kt:72)
2. Re-enable backface culling if desired (WebGPURenderer.kt:405)
3. Remove diagnostic logging:
   - WebGPURenderer.kt:467-476 (vertex color logging)
   - WebGPURenderer.kt:582-620 (matrix logging)
   - Main.kt:254-261, 287-293 (frame logging)

## Quick Test Commands
```bash
# Build and run (creates new port)
./gradlew :examples:voxelcraft:jsBrowserDevelopmentRun -x jsNodeTest

# Check logs
cat docs/log.md | grep "T021"

# View in browser
# http://localhost:<PORT>
```

## Expected Fix Impact
Once view matrix is correctly computed:
- Camera at (0, 100, 0) looking down should see terrain
- Terrain generates at Y=64-124, so should be visible
- Screen should show red cubes (due to fragment shader override)
- After confirming rendering works, revert diagnostic changes to see proper colors
