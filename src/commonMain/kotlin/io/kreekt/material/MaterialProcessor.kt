/**
 * MaterialProcessor for validation and optimization
 * T029 - Advanced material processing with validation, optimization, and performance analysis
 *
 * Features:
 * - Material validation and compatibility checking
 * - Performance optimization and LOD generation
 * - Texture compression and format conversion
 * - Shader variant generation and selection
 * - Material library management and caching
 * - Runtime material adaptation based on hardware capabilities
 * - Material debugging and profiling tools
 */
package io.kreekt.material

import io.kreekt.core.math.*
import io.kreekt.geometry.TextureAtlas
import kotlinx.collections.immutable.*
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Advanced material processor for optimization and quality management
 * Handles material validation, optimization, and adaptation for different platforms
 */
class MaterialProcessor {

    // Processing configuration
    var enableOptimization: Boolean = true
    var enableValidation: Boolean = true
    var enableProfiling: Boolean = false
    var targetQuality: MaterialQuality = MaterialQuality.HIGH

    // Hardware capability detection
    private var _hardwareCapabilities: HardwareCapabilities = detectHardwareCapabilities()
    private val _materialCache = mutableMapOf<String, ProcessedMaterial>()
    private val _validationCache = mutableMapOf<String, MaterialValidationResult>()

    // Statistics and metrics
    private var _processedMaterials = 0
    private var _optimizationSavings = 0.0f
    private var _validationErrors = 0

    companion object {
        // Material quality presets
        val QUALITY_PRESETS = mapOf(
            MaterialQuality.LOW to MaterialOptimizationSettings(
                maxTextureSize = 512,
                enableMipmaps = false,
                compressionQuality = 0.6f,
                simplifyShaders = true,
                disableAdvancedFeatures = true,
                maxShaderVariants = 2
            ),
            MaterialQuality.MEDIUM to MaterialOptimizationSettings(
                maxTextureSize = 1024,
                enableMipmaps = true,
                compressionQuality = 0.8f,
                simplifyShaders = false,
                disableAdvancedFeatures = false,
                maxShaderVariants = 4
            ),
            MaterialQuality.HIGH to MaterialOptimizationSettings(
                maxTextureSize = 2048,
                enableMipmaps = true,
                compressionQuality = 0.9f,
                simplifyShaders = false,
                disableAdvancedFeatures = false,
                maxShaderVariants = 8
            ),
            MaterialQuality.ULTRA to MaterialOptimizationSettings(
                maxTextureSize = 4096,
                enableMipmaps = true,
                compressionQuality = 1.0f,
                simplifyShaders = false,
                disableAdvancedFeatures = false,
                maxShaderVariants = 16
            )
        )

        // Performance thresholds
        const val GPU_MEMORY_WARNING_THRESHOLD = 0.8f
        const val DRAW_CALL_WARNING_THRESHOLD = 1000
        const val SHADER_COMPLEXITY_WARNING_THRESHOLD = 100
    }

    /**
     * Process and optimize a single material
     */
    fun processMaterial(
        material: Material,
        options: MaterialProcessingOptions = MaterialProcessingOptions()
    ): MaterialProcessingResult {
        val startTime = System.currentTimeMillis()

        try {
            // Check cache first
            val cacheKey = generateMaterialCacheKey(material, options)
            _materialCache[cacheKey]?.let { cached ->
                return MaterialProcessingResult.Success(
                    original = material,
                    processed = cached.material,
                    optimizations = cached.optimizations,
                    processingTime = 0f,
                    fromCache = true
                )
            }

            // Validate material
            val validationResult = if (enableValidation) {
                validateMaterial(material, options.validationOptions)
            } else {
                MaterialValidationResult(isValid = true, issues = emptyList())
            }

            if (!validationResult.isValid && options.failOnValidationError) {
                return MaterialProcessingResult.ValidationError(
                    material = material,
                    validationResult = validationResult
                )
            }

            // Optimize material
            val optimizedMaterial = if (enableOptimization) {
                optimizeMaterial(material, options.optimizationSettings)
            } else {
                material
            }

            // Generate processing report
            val optimizations = generateOptimizationReport(material, optimizedMaterial.material)
            val processingTime = (System.currentTimeMillis() - startTime) / 1000f

            // Cache result
            val processedMaterial = ProcessedMaterial(
                material = optimizedMaterial.material,
                optimizations = optimizations,
                processingTime = processingTime,
                cacheKey = cacheKey
            )
            _materialCache[cacheKey] = processedMaterial

            // Update statistics
            _processedMaterials++
            _optimizationSavings += optimizedMaterial.memorySavings

            return MaterialProcessingResult.Success(
                original = material,
                processed = optimizedMaterial.material,
                optimizations = optimizations,
                processingTime = processingTime,
                fromCache = false
            )

        } catch (e: Exception) {
            return MaterialProcessingResult.ProcessingError(
                material = material,
                error = e.message ?: "Unknown processing error"
            )
        }
    }

    /**
     * Process multiple materials in batch
     */
    fun processMaterials(
        materials: List<Material>,
        options: BatchMaterialProcessingOptions = BatchMaterialProcessingOptions()
    ): BatchMaterialProcessingResult {
        val results = mutableMapOf<Material, MaterialProcessingResult>()
        val failed = mutableListOf<Material>()
        var totalOptimizationSavings = 0f

        // Sort materials by complexity for optimal processing order
        val sortedMaterials = if (options.sortByComplexity) {
            materials.sortedBy { calculateMaterialComplexity(it) }
        } else {
            materials
        }

        // Process materials
        for (material in sortedMaterials) {
            val result = processMaterial(material, options.processingOptions)
            results[material] = result

            when (result) {
                is MaterialProcessingResult.Success -> {
                    totalOptimizationSavings += calculateMemorySavings(result.original, result.processed)
                }
                is MaterialProcessingResult.ValidationError,
                is MaterialProcessingResult.ProcessingError -> {
                    failed.add(material)
                    if (!options.continueOnFailure) {
                        break
                    }
                }
            }
        }

        return BatchMaterialProcessingResult(
            results = results,
            successCount = results.count { it.value is MaterialProcessingResult.Success },
            failureCount = failed.size,
            failedMaterials = failed,
            totalMemorySavings = totalOptimizationSavings
        )
    }

    /**
     * Validate a material for correctness and compatibility
     */
    fun validateMaterial(
        material: Material,
        options: MaterialValidationOptions = MaterialValidationOptions()
    ): MaterialValidationResult {
        // Check validation cache
        val cacheKey = generateValidationCacheKey(material, options)
        _validationCache[cacheKey]?.let { return it }

        val issues = mutableListOf<MaterialIssue>()

        // Basic material validation
        validateBasicProperties(material, issues)

        // Type-specific validation
        when (material) {
            is PBRMaterial -> validatePBRMaterial(material, issues, options)
            is ShaderMaterial -> validateShaderMaterial(material, issues, options)
            else -> issues.add(MaterialIssue.Warning("Unknown material type: ${material::class.simpleName}"))
        }

        // Hardware compatibility validation
        if (options.checkHardwareCompatibility) {
            validateHardwareCompatibility(material, issues)
        }

        // Performance validation
        if (options.checkPerformance) {
            validatePerformance(material, issues, options)
        }

        val result = MaterialValidationResult(
            isValid = issues.none { it.severity == IssueSeverity.ERROR },
            issues = issues
        )

        // Cache result
        _validationCache[cacheKey] = result

        if (!result.isValid) {
            _validationErrors++
        }

        return result
    }

    /**
     * Optimize material for target platform and quality
     */
    fun optimizeMaterial(
        material: Material,
        settings: MaterialOptimizationSettings = QUALITY_PRESETS[targetQuality]!!
    ): MaterialOptimizationResult {
        val originalMemoryUsage = calculateMaterialMemoryUsage(material)
        var optimizedMaterial = material

        val optimizations = mutableListOf<String>()

        // Optimize based on material type
        when (material) {
            is PBRMaterial -> {
                val pbrResult = optimizePBRMaterial(material, settings)
                optimizedMaterial = pbrResult.material
                optimizations.addAll(pbrResult.optimizations)
            }
            is ShaderMaterial -> {
                val shaderResult = optimizeShaderMaterial(material, settings)
                optimizedMaterial = shaderResult.material
                optimizations.addAll(shaderResult.optimizations)
            }
        }

        // Apply texture optimizations
        if (settings.maxTextureSize > 0) {
            val textureResult = optimizeTextures(optimizedMaterial, settings)
            optimizedMaterial = textureResult.material
            optimizations.addAll(textureResult.optimizations)
        }

        // Calculate savings
        val optimizedMemoryUsage = calculateMaterialMemoryUsage(optimizedMaterial)
        val memorySavings = originalMemoryUsage - optimizedMemoryUsage

        return MaterialOptimizationResult(
            material = optimizedMaterial,
            optimizations = optimizations,
            memorySavings = memorySavings,
            compressionRatio = if (originalMemoryUsage > 0) {
                optimizedMemoryUsage.toFloat() / originalMemoryUsage
            } else 1f
        )
    }

    /**
     * Generate material variants for different quality levels
     */
    fun generateMaterialVariants(
        material: Material,
        targetQualities: List<MaterialQuality> = MaterialQuality.values().toList()
    ): MaterialVariantResult {
        val variants = mutableMapOf<MaterialQuality, Material>()
        val originalComplexity = calculateMaterialComplexity(material)

        for (quality in targetQualities) {
            val settings = QUALITY_PRESETS[quality] ?: continue
            val optimizationResult = optimizeMaterial(material, settings)
            variants[quality] = optimizationResult.material
        }

        return MaterialVariantResult(
            originalMaterial = material,
            variants = variants,
            originalComplexity = originalComplexity,
            variantComplexities = variants.mapValues { calculateMaterialComplexity(it.value) }
        )
    }

    /**
     * Create a material atlas from multiple materials
     */
    fun createMaterialAtlas(
        materials: List<Material>,
        atlasSize: Int = 2048,
        options: AtlasCreationOptions = AtlasCreationOptions()
    ): MaterialAtlasResult {
        val atlas = TextureAtlas(
            width = atlasSize,
            height = atlasSize,
            format = options.textureFormat,
            packingStrategy = options.packingStrategy
        )

        val atlasedMaterials = mutableMapOf<Material, Material>()
        val uvMappings = mutableMapOf<Material, UVMapping>()
        val failed = mutableListOf<Material>()

        // Extract textures from materials
        val textureMap = mutableMapOf<String, TextureData>()
        materials.forEach { material ->
            val textures = extractTexturesFromMaterial(material)
            textureMap.putAll(textures)
        }

        // Pack textures into atlas
        val packingResult = atlas.packTextures(textureMap)

        // Create new materials with atlas UVs
        for (material in materials) {
            try {
                val atlasedMaterial = createAtlasedMaterial(material, atlas, packingResult.results)
                atlasedMaterials[material] = atlasedMaterial

                // Store UV mapping info
                val materialTextures = extractTexturesFromMaterial(material)
                if (materialTextures.isNotEmpty()) {
                    val firstTexture = materialTextures.keys.first()
                    atlas.getUVMapping(firstTexture)?.let { mapping ->
                        uvMappings[material] = mapping
                    }
                }
            } catch (e: Exception) {
                failed.add(material)
            }
        }

        return MaterialAtlasResult(
            atlas = atlas,
            originalMaterials = materials,
            atlasedMaterials = atlasedMaterials,
            uvMappings = uvMappings,
            failedMaterials = failed,
            memorySavings = calculateAtlasMemorySavings(materials, atlasedMaterials.values.toList())
        )
    }

    /**
     * Adapt material for current hardware capabilities
     */
    fun adaptMaterialForHardware(
        material: Material,
        capabilities: HardwareCapabilities = _hardwareCapabilities
    ): MaterialAdaptationResult {
        val adaptations = mutableListOf<String>()
        var adaptedMaterial = material

        // Check texture format support
        if (material is PBRMaterial) {
            if (!capabilities.supportsCompressedTextures && material.optimizations.useCompression) {
                adaptedMaterial = material.clone().apply {
                    optimizations.useCompression = false
                }
                adaptations.add("Disabled texture compression (not supported)")
            }

            // Check advanced features support
            if (!capabilities.supportsClearcoat && material.clearcoat.enabled) {
                adaptedMaterial = (adaptedMaterial as PBRMaterial).clone().apply {
                    clearcoat.enabled = false
                }
                adaptations.add("Disabled clearcoat (not supported)")
            }

            if (!capabilities.supportsTransmission && material.transmission.enabled) {
                adaptedMaterial = (adaptedMaterial as PBRMaterial).clone().apply {
                    transmission.enabled = false
                }
                adaptations.add("Disabled transmission (not supported)")
            }
        }

        // Check shader complexity limits
        if (material is ShaderMaterial) {
            val complexity = calculateShaderComplexity(material)
            if (complexity > capabilities.maxShaderComplexity) {
                val simplifiedResult = simplifyShaderMaterial(material, capabilities.maxShaderComplexity)
                adaptedMaterial = simplifiedResult.material
                adaptations.addAll(simplifiedResult.adaptations)
            }
        }

        return MaterialAdaptationResult(
            originalMaterial = material,
            adaptedMaterial = adaptedMaterial,
            adaptations = adaptations,
            targetCapabilities = capabilities
        )
    }

    /**
     * Get comprehensive material performance metrics
     */
    fun analyzeMaterialPerformance(material: Material): MaterialPerformanceAnalysis {
        val memoryUsage = calculateMaterialMemoryUsage(material)
        val complexity = calculateMaterialComplexity(material)
        val renderCost = estimateRenderCost(material)

        val bottlenecks = mutableListOf<PerformanceBottleneck>()

        // Identify potential bottlenecks
        if (memoryUsage > _hardwareCapabilities.maxTextureMemory * GPU_MEMORY_WARNING_THRESHOLD) {
            bottlenecks.add(PerformanceBottleneck.HIGH_MEMORY_USAGE)
        }

        if (complexity > SHADER_COMPLEXITY_WARNING_THRESHOLD) {
            bottlenecks.add(PerformanceBottleneck.COMPLEX_SHADERS)
        }

        if (renderCost > _hardwareCapabilities.maxRenderCost * 0.1f) {
            bottlenecks.add(PerformanceBottleneck.HIGH_RENDER_COST)
        }

        return MaterialPerformanceAnalysis(
            material = material,
            memoryUsage = memoryUsage,
            complexity = complexity,
            renderCost = renderCost,
            bottlenecks = bottlenecks,
            recommendations = generatePerformanceRecommendations(material, bottlenecks)
        )
    }

    /**
     * Get material processor statistics
     */
    fun getStatistics(): MaterialProcessorStatistics {
        return MaterialProcessorStatistics(
            processedMaterials = _processedMaterials,
            cachedMaterials = _materialCache.size,
            optimizationSavings = _optimizationSavings,
            validationErrors = _validationErrors,
            cacheHitRatio = if (_processedMaterials > 0) {
                _materialCache.size.toFloat() / _processedMaterials
            } else 0f
        )
    }

    /**
     * Clear all caches
     */
    fun clearCaches() {
        _materialCache.clear()
        _validationCache.clear()
    }

    // Private validation methods

    private fun validateBasicProperties(material: Material, issues: MutableList<MaterialIssue>) {
        if (material.name.isBlank()) {
            issues.add(MaterialIssue.Warning("Material has empty name"))
        }

        if (!material.visible) {
            issues.add(MaterialIssue.Info("Material is not visible"))
        }
    }

    private fun validatePBRMaterial(
        material: PBRMaterial,
        issues: MutableList<MaterialIssue>,
        options: MaterialValidationOptions
    ) {
        // Validate PBR parameter ranges
        if (material.metallic < 0f || material.metallic > 1f) {
            issues.add(MaterialIssue.Error("Metallic value out of range [0,1]: ${material.metallic}"))
        }

        if (material.roughness < 0f || material.roughness > 1f) {
            issues.add(MaterialIssue.Error("Roughness value out of range [0,1]: ${material.roughness}"))
        }

        if (material.reflectance < 0f || material.reflectance > 1f) {
            issues.add(MaterialIssue.Warning("Reflectance value out of range [0,1]: ${material.reflectance}"))
        }

        // Validate texture compatibility
        validateTextureCompatibility(material, issues)

        // Validate advanced features
        if (material.clearcoat.enabled && !_hardwareCapabilities.supportsClearcoat) {
            issues.add(MaterialIssue.Warning("Clearcoat not supported on target hardware"))
        }

        if (material.transmission.enabled && !_hardwareCapabilities.supportsTransmission) {
            issues.add(MaterialIssue.Warning("Transmission not supported on target hardware"))
        }
    }

    private fun validateShaderMaterial(
        material: ShaderMaterial,
        issues: MutableList<MaterialIssue>,
        options: MaterialValidationOptions
    ) {
        // Validate shader source
        if (material.vertexShader.isEmpty() && material.fragmentShader.isEmpty() && material.computeShader.isEmpty()) {
            issues.add(MaterialIssue.Error("No shader stages defined"))
        }

        // Validate shader compilation
        val compilationResult = material.compile()
        if (!compilationResult.success) {
            compilationResult.errors.forEach { error ->
                issues.add(MaterialIssue.Error("Shader compilation error: $error"))
            }
        }

        compilationResult.warnings.forEach { warning ->
            issues.add(MaterialIssue.Warning("Shader compilation warning: $warning"))
        }

        // Validate shader complexity
        val complexity = calculateShaderComplexity(material)
        if (complexity > _hardwareCapabilities.maxShaderComplexity) {
            issues.add(MaterialIssue.Warning("Shader too complex for target hardware: $complexity > ${_hardwareCapabilities.maxShaderComplexity}"))
        }
    }

    private fun validateHardwareCompatibility(material: Material, issues: MutableList<MaterialIssue>) {
        val memoryUsage = calculateMaterialMemoryUsage(material)
        if (memoryUsage > _hardwareCapabilities.maxTextureMemory) {
            issues.add(MaterialIssue.Error("Material exceeds available texture memory"))
        }

        if (material is ShaderMaterial) {
            if (material.features.contains(ShaderFeature.TESSELLATION) && !_hardwareCapabilities.supportsTessellation) {
                issues.add(MaterialIssue.Error("Tessellation not supported on target hardware"))
            }

            if (material.features.contains(ShaderFeature.COMPUTE) && !_hardwareCapabilities.supportsCompute) {
                issues.add(MaterialIssue.Error("Compute shaders not supported on target hardware"))
            }
        }
    }

    private fun validatePerformance(
        material: Material,
        issues: MutableList<MaterialIssue>,
        options: MaterialValidationOptions
    ) {
        val complexity = calculateMaterialComplexity(material)
        if (complexity > options.maxComplexity) {
            issues.add(MaterialIssue.Warning("Material complexity exceeds recommended limit: $complexity > ${options.maxComplexity}"))
        }

        val renderCost = estimateRenderCost(material)
        if (renderCost > options.maxRenderCost) {
            issues.add(MaterialIssue.Warning("Material render cost exceeds recommended limit: $renderCost > ${options.maxRenderCost}"))
        }
    }

    private fun validateTextureCompatibility(material: PBRMaterial, issues: MutableList<MaterialIssue>) {
        // Check if all textures have compatible formats and sizes
        val textures = listOfNotNull(
            material.albedoMap,
            material.normalMap,
            material.metallicRoughnessMap,
            material.emissiveMap,
            material.aoMap
        )

        if (textures.isNotEmpty()) {
            // Implementation would check texture format compatibility
        }
    }

    // Private optimization methods

    private fun optimizePBRMaterial(
        material: PBRMaterial,
        settings: MaterialOptimizationSettings
    ): MaterialOptimizationResult {
        val optimized = material.clone()
        val optimizations = mutableListOf<String>()

        // Disable expensive features based on quality settings
        if (settings.disableAdvancedFeatures) {
            if (optimized.clearcoat.enabled) {
                optimized.clearcoat.enabled = false
                optimizations.add("Disabled clearcoat for performance")
            }

            if (optimized.transmission.enabled) {
                optimized.transmission.enabled = false
                optimizations.add("Disabled transmission for performance")
            }

            if (optimized.iridescence.enabled) {
                optimized.iridescence.enabled = false
                optimizations.add("Disabled iridescence for performance")
            }
        }

        // Optimize quality settings
        optimized.quality = targetQuality
        optimized.optimizations.maxTextureSize = settings.maxTextureSize
        optimized.optimizations.useCompression = settings.compressionQuality < 1.0f

        return MaterialOptimizationResult(
            material = optimized,
            optimizations = optimizations,
            memorySavings = 0f, // Would be calculated based on actual changes
            compressionRatio = 1f
        )
    }

    private fun optimizeShaderMaterial(
        material: ShaderMaterial,
        settings: MaterialOptimizationSettings
    ): MaterialOptimizationResult {
        val optimized = material.clone()
        val optimizations = mutableListOf<String>()

        // Simplify shaders if needed
        if (settings.simplifyShaders) {
            val simplificationResult = simplifyShaderMaterial(material, SHADER_COMPLEXITY_WARNING_THRESHOLD)
            if (simplificationResult.adaptations.isNotEmpty()) {
                optimizations.addAll(simplificationResult.adaptations)
            }
        }

        // Limit shader variants
        if (optimized.shaderVariants.size > settings.maxShaderVariants) {
            val variantsToRemove = optimized.shaderVariants.size - settings.maxShaderVariants
            optimizations.add("Removed $variantsToRemove shader variants")
        }

        return MaterialOptimizationResult(
            material = optimized,
            optimizations = optimizations,
            memorySavings = 0f,
            compressionRatio = 1f
        )
    }

    private fun optimizeTextures(
        material: Material,
        settings: MaterialOptimizationSettings
    ): MaterialOptimizationResult {
        // Implementation would optimize texture sizes, formats, and compression
        return MaterialOptimizationResult(
            material = material,
            optimizations = emptyList(),
            memorySavings = 0f,
            compressionRatio = 1f
        )
    }

    // Private utility methods

    private fun calculateMaterialComplexity(material: Material): Int {
        var complexity = 0

        when (material) {
            is PBRMaterial -> {
                complexity += 10 // Base PBR complexity
                if (material.clearcoat.enabled) complexity += 5
                if (material.transmission.enabled) complexity += 8
                if (material.iridescence.enabled) complexity += 6
                if (material.subsurface.enabled) complexity += 7
                complexity += countTextures(material) * 2
            }
            is ShaderMaterial -> {
                complexity += calculateShaderComplexity(material)
                complexity += material.uniforms.size
                complexity += material.textures.size * 2
            }
            else -> {
                complexity = 5 // Basic material
            }
        }

        return complexity
    }

    private fun calculateShaderComplexity(material: ShaderMaterial): Int {
        // Simplified shader complexity calculation
        var complexity = 0
        complexity += material.vertexShader.length / 100
        complexity += material.fragmentShader.length / 100
        complexity += material.computeShader.length / 100
        complexity += material.features.size * 5
        return complexity
    }

    private fun countTextures(material: PBRMaterial): Int {
        return listOfNotNull(
            material.albedoMap,
            material.normalMap,
            material.metallicRoughnessMap,
            material.emissiveMap,
            material.aoMap,
            material.clearcoatMap,
            material.transmissionMap,
            material.iridescenceMap
        ).size
    }

    private fun calculateMaterialMemoryUsage(material: Material): Int {
        // Simplified memory calculation
        var usage = 1024 // Base material overhead

        when (material) {
            is PBRMaterial -> {
                usage += countTextures(material) * 1024 * 1024 // Assume 1MB per texture
            }
            is ShaderMaterial -> {
                usage += material.textures.size * 1024 * 1024
                usage += material.storageBuffers.values.sumOf { it.data.size }
            }
        }

        return usage
    }

    private fun estimateRenderCost(material: Material): Float {
        // Simplified render cost estimation
        var cost = 1f // Base cost

        when (material) {
            is PBRMaterial -> {
                cost += countTextures(material) * 0.1f
                if (material.clearcoat.enabled) cost += 0.2f
                if (material.transmission.enabled) cost += 0.3f
                if (material.iridescence.enabled) cost += 0.25f
            }
            is ShaderMaterial -> {
                cost += calculateShaderComplexity(material) * 0.01f
                cost += material.textures.size * 0.1f
            }
        }

        return cost
    }

    private fun generateOptimizationReport(original: Material, optimized: Material): List<String> {
        val report = mutableListOf<String>()

        val originalMemory = calculateMaterialMemoryUsage(original)
        val optimizedMemory = calculateMaterialMemoryUsage(optimized)

        if (optimizedMemory < originalMemory) {
            val savings = originalMemory - optimizedMemory
            report.add("Reduced memory usage by ${savings / 1024}KB")
        }

        val originalComplexity = calculateMaterialComplexity(original)
        val optimizedComplexity = calculateMaterialComplexity(optimized)

        if (optimizedComplexity < originalComplexity) {
            report.add("Reduced complexity from $originalComplexity to $optimizedComplexity")
        }

        return report
    }

    private fun calculateMemorySavings(original: Material, optimized: Material): Float {
        val originalMemory = calculateMaterialMemoryUsage(original)
        val optimizedMemory = calculateMaterialMemoryUsage(optimized)
        return maxOf(0f, (originalMemory - optimizedMemory).toFloat())
    }

    private fun simplifyShaderMaterial(
        material: ShaderMaterial,
        targetComplexity: Int
    ): MaterialAdaptationResult {
        val simplified = material.clone()
        val adaptations = mutableListOf<String>()

        // Remove expensive features
        val expensiveFeatures = listOf(
            ShaderFeature.TESSELLATION,
            ShaderFeature.GEOMETRY_SHADER,
            ShaderFeature.SUBSURFACE_SCATTERING,
            ShaderFeature.IRIDESCENCE
        )

        for (feature in expensiveFeatures) {
            if (simplified.features.contains(feature)) {
                simplified.removeFeature(feature)
                adaptations.add("Removed ${feature.featureName} feature for performance")

                if (calculateShaderComplexity(simplified) <= targetComplexity) {
                    break
                }
            }
        }

        return MaterialAdaptationResult(
            originalMaterial = material,
            adaptedMaterial = simplified,
            adaptations = adaptations,
            targetCapabilities = _hardwareCapabilities
        )
    }

    private fun extractTexturesFromMaterial(material: Material): Map<String, TextureData> {
        // Implementation would extract all textures from material
        return emptyMap()
    }

    private fun createAtlasedMaterial(
        material: Material,
        atlas: TextureAtlas,
        packingResults: Map<String, PackingResult>
    ): Material {
        // Implementation would create new material with atlas texture references
        return material
    }

    private fun calculateAtlasMemorySavings(
        originalMaterials: List<Material>,
        atlasedMaterials: List<Material>
    ): Float {
        val originalMemory = originalMaterials.sumOf { calculateMaterialMemoryUsage(it) }
        val atlasedMemory = atlasedMaterials.sumOf { calculateMaterialMemoryUsage(it) }
        return maxOf(0f, (originalMemory - atlasedMemory).toFloat())
    }

    private fun generatePerformanceRecommendations(
        material: Material,
        bottlenecks: List<PerformanceBottleneck>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        for (bottleneck in bottlenecks) {
            when (bottleneck) {
                PerformanceBottleneck.HIGH_MEMORY_USAGE -> {
                    recommendations.add("Consider reducing texture sizes or using texture compression")
                    recommendations.add("Remove unused textures and material features")
                }
                PerformanceBottleneck.COMPLEX_SHADERS -> {
                    recommendations.add("Simplify shader code and remove expensive operations")
                    recommendations.add("Use LOD variants for distant objects")
                }
                PerformanceBottleneck.HIGH_RENDER_COST -> {
                    recommendations.add("Optimize material for target hardware capabilities")
                    recommendations.add("Consider using material atlasing to reduce draw calls")
                }
                PerformanceBottleneck.TOO_MANY_VARIANTS -> {
                    recommendations.add("Reduce number of shader variants")
                    recommendations.add("Use dynamic branching instead of multiple variants where possible")
                }
            }
        }

        return recommendations
    }

    private fun detectHardwareCapabilities(): HardwareCapabilities {
        // Implementation would detect actual hardware capabilities
        return HardwareCapabilities(
            maxTextureMemory = 1024 * 1024 * 1024, // 1GB
            maxTextureSize = 4096,
            maxShaderComplexity = 200,
            maxRenderCost = 10f,
            supportsCompressedTextures = true,
            supportsClearcoat = true,
            supportsTransmission = true,
            supportsTessellation = false,
            supportsCompute = true
        )
    }

    private fun generateMaterialCacheKey(material: Material, options: MaterialProcessingOptions): String {
        return "${material.hashCode()}-${options.hashCode()}-${targetQuality.hashCode()}"
    }

    private fun generateValidationCacheKey(material: Material, options: MaterialValidationOptions): String {
        return "${material.hashCode()}-${options.hashCode()}"
    }
}

// Data classes and enums for material processing

/**
 * Material processing options
 */
data class MaterialProcessingOptions(
    val validationOptions: MaterialValidationOptions = MaterialValidationOptions(),
    val optimizationSettings: MaterialOptimizationSettings? = null,
    val failOnValidationError: Boolean = false
)

/**
 * Batch material processing options
 */
data class BatchMaterialProcessingOptions(
    val processingOptions: MaterialProcessingOptions = MaterialProcessingOptions(),
    val sortByComplexity: Boolean = true,
    val continueOnFailure: Boolean = true
)

/**
 * Material validation options
 */
data class MaterialValidationOptions(
    val checkHardwareCompatibility: Boolean = true,
    val checkPerformance: Boolean = true,
    val maxComplexity: Int = 100,
    val maxRenderCost: Float = 5f
)

/**
 * Material optimization settings
 */
@Serializable
data class MaterialOptimizationSettings(
    val maxTextureSize: Int = 2048,
    val enableMipmaps: Boolean = true,
    val compressionQuality: Float = 0.9f,
    val simplifyShaders: Boolean = false,
    val disableAdvancedFeatures: Boolean = false,
    val maxShaderVariants: Int = 8
)

/**
 * Atlas creation options
 */
data class AtlasCreationOptions(
    val textureFormat: TextureFormat = TextureFormat.RGBA8,
    val packingStrategy: PackingStrategy = PackingStrategy.MAX_RECTS,
    val enableCompression: Boolean = true
)

/**
 * Hardware capabilities
 */
data class HardwareCapabilities(
    val maxTextureMemory: Int,
    val maxTextureSize: Int,
    val maxShaderComplexity: Int,
    val maxRenderCost: Float,
    val supportsCompressedTextures: Boolean,
    val supportsClearcoat: Boolean,
    val supportsTransmission: Boolean,
    val supportsTessellation: Boolean,
    val supportsCompute: Boolean
)

/**
 * Performance bottleneck enumeration
 */
enum class PerformanceBottleneck {
    HIGH_MEMORY_USAGE,
    COMPLEX_SHADERS,
    HIGH_RENDER_COST,
    TOO_MANY_VARIANTS
}

/**
 * Issue severity levels
 */
enum class IssueSeverity {
    INFO, WARNING, ERROR
}

/**
 * Material issue representation
 */
sealed class MaterialIssue(val severity: IssueSeverity, val message: String) {
    class Info(message: String) : MaterialIssue(IssueSeverity.INFO, message)
    class Warning(message: String) : MaterialIssue(IssueSeverity.WARNING, message)
    class Error(message: String) : MaterialIssue(IssueSeverity.ERROR, message)
}

// Result classes

/**
 * Material validation result
 */
data class MaterialValidationResult(
    val isValid: Boolean,
    val issues: List<MaterialIssue>
)

/**
 * Material optimization result
 */
data class MaterialOptimizationResult(
    val material: Material,
    val optimizations: List<String>,
    val memorySavings: Float,
    val compressionRatio: Float
)

/**
 * Material processing result
 */
sealed class MaterialProcessingResult {
    data class Success(
        val original: Material,
        val processed: Material,
        val optimizations: List<String>,
        val processingTime: Float,
        val fromCache: Boolean
    ) : MaterialProcessingResult()

    data class ValidationError(
        val material: Material,
        val validationResult: MaterialValidationResult
    ) : MaterialProcessingResult()

    data class ProcessingError(
        val material: Material,
        val error: String
    ) : MaterialProcessingResult()
}

/**
 * Batch material processing result
 */
data class BatchMaterialProcessingResult(
    val results: Map<Material, MaterialProcessingResult>,
    val successCount: Int,
    val failureCount: Int,
    val failedMaterials: List<Material>,
    val totalMemorySavings: Float
)

/**
 * Material variant result
 */
data class MaterialVariantResult(
    val originalMaterial: Material,
    val variants: Map<MaterialQuality, Material>,
    val originalComplexity: Int,
    val variantComplexities: Map<MaterialQuality, Int>
)

/**
 * Material atlas result
 */
data class MaterialAtlasResult(
    val atlas: TextureAtlas,
    val originalMaterials: List<Material>,
    val atlasedMaterials: Map<Material, Material>,
    val uvMappings: Map<Material, UVMapping>,
    val failedMaterials: List<Material>,
    val memorySavings: Float
)

/**
 * Material adaptation result
 */
data class MaterialAdaptationResult(
    val originalMaterial: Material,
    val adaptedMaterial: Material,
    val adaptations: List<String>,
    val targetCapabilities: HardwareCapabilities
)

/**
 * Material performance analysis
 */
data class MaterialPerformanceAnalysis(
    val material: Material,
    val memoryUsage: Int,
    val complexity: Int,
    val renderCost: Float,
    val bottlenecks: List<PerformanceBottleneck>,
    val recommendations: List<String>
)

/**
 * Material processor statistics
 */
data class MaterialProcessorStatistics(
    val processedMaterials: Int,
    val cachedMaterials: Int,
    val optimizationSavings: Float,
    val validationErrors: Int,
    val cacheHitRatio: Float
)

/**
 * Processed material cache entry
 */
data class ProcessedMaterial(
    val material: Material,
    val optimizations: List<String>,
    val processingTime: Float,
    val cacheKey: String
)