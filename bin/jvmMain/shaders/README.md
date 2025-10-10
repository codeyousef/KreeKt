# Compiled Shaders (SPIR-V)

This directory contains SPIR-V bytecode compiled from WGSL source shaders.

## Source

- WGSL source: `src/commonMain/resources/shaders/*.wgsl`
- Compilation guide: `buildSrc/SHADER_COMPILATION.md`

## Files

- `basic.spv` - Basic vertex + fragment shader (SPIR-V bytecode)

## Compilation

To recompile shaders:

```bash
# Using Tint (recommended)
tint --format spirv src/commonMain/resources/shaders/basic.wgsl -o src/jvmMain/resources/shaders/basic.spv

# Using naga
naga src/commonMain/resources/shaders/basic.wgsl src/jvmMain/resources/shaders/basic.spv
```

## Note

For MVP, SPIR-V files should be compiled manually and checked into version control.
Automated Gradle task deferred to post-MVP (see T032 in tasks.md).
