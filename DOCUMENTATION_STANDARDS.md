# KreeKt Documentation Standards

Quick reference for writing professional KDoc documentation in KreeKt.

## Documentation Template

### Class Documentation

```kotlin
/**
 * # ClassName - Brief One-Line Description
 *
 * Longer description explaining what this class does and why it exists.
 * 2-3 sentences providing context.
 *
 * ## Overview
 *
 * Bullet-point list of capabilities:
 * - **Feature 1**: Description
 * - **Feature 2**: Description
 * - **Feature 3**: Description
 *
 * ## Basic Usage
 *
 * ```kotlin
 * // Simple, practical example
 * val instance = ClassName()
 * instance.doSomething()
 * ```
 *
 * ## Advanced Usage
 *
 * ```kotlin
 * // More complex example showing advanced features
 * val instance = ClassName {
 *     configure()
 * }
 * ```
 *
 * ## Performance Considerations
 *
 * - Performance tip 1
 * - Performance tip 2
 *
 * @property propertyName Description of the property and its purpose
 * @property anotherProperty Another property description
 *
 * @constructor Creates instance with specified parameters
 *
 * @see RelatedClass
 * @see AnotherRelatedClass
 *
 * @since 1.0.0
 * @sample io.kreekt.samples.ClassNameSamples.basicUsage
 */
class ClassName {
    // Implementation
}
```

### Method Documentation

```kotlin
/**
 * Brief one-line description of what the method does.
 *
 * Longer explanation if needed, covering:
 * - What the method does
 * - When to use it
 * - Important considerations
 *
 * Example:
 * ```kotlin
 * val result = instance.methodName(param1, param2)
 * println(result)
 * ```
 *
 * @param param1 Description of first parameter
 * @param param2 Description of second parameter
 * @return Description of return value
 * @throws ExceptionType When this exception is thrown
 * @see RelatedMethod
 * @since 1.0.0
 */
fun methodName(param1: Type1, param2: Type2): ReturnType {
    // Implementation
}
```

### Property Documentation

```kotlin
/**
 * Description of what this property represents.
 *
 * Additional context about usage, valid values, or implications.
 *
 * @see RelatedProperty
 * @since 1.0.0
 */
var propertyName: Type = defaultValue
```

## Documentation Sections

### Required for All Public APIs
- Brief one-line summary (first sentence)
- @since version tag

### Required for Classes
- # Heading with class name and brief description
- ## Overview section with bullet points
- ## Basic Usage with code example
- @property tags for all public properties
- @see tags for related classes
- @sample reference to example file

### Required for Methods
- Brief description
- @param for each parameter
- @return for non-void methods
- @throws for checked exceptions
- Example code block (for complex methods)

### Optional but Recommended
- ## Performance Considerations
- ## Platform Notes
- ## Advanced Usage
- Multiple code examples
- Links to user guide sections

## Writing Style Guidelines

### Clarity
- Start with the most important information
- Use active voice ("Calculates X" not "X is calculated")
- Be concise but complete
- Define technical terms on first use

### Examples
- Show realistic, practical usage
- Include complete, runnable code where possible
- Use meaningful variable names
- Add comments to explain non-obvious parts

### Structure
- One idea per paragraph
- Use bullet points for lists
- Break long descriptions into sections
- Put examples after explanations

## Markdown in KDoc

### Supported Formatting
- **Bold**: `**text**`
- *Italic*: `*text*`
- `Code`: `` `code` ``
- Code blocks: `` ```kotlin ... ``` ``
- Headers: `## Header`
- Lists: `- Item` or `1. Item`
- Links: `[Text](URL)`

### Special Tags
- `@param name` - Parameter description
- `@return` - Return value description
- `@throws Exception` - Exception conditions
- `@property name` - Property description
- `@constructor` - Constructor description
- `@see` - Cross-reference
- `@sample` - Reference to sample code
- `@since` - Version introduced
- `@deprecated` - Deprecation notice

## Code Example Best Practices

### Good Example
```kotlin
/**
 * Transforms a point from local to world space.
 *
 * Example:
 * ```kotlin
 * val localPoint = Vector3(5f, 0f, 0f)
 * val worldPoint = object3D.localToWorld(localPoint)
 * println("World position: $worldPoint")
 * ```
 */
```

### Bad Example
```kotlin
/**
 * Does local to world transform.
 */
```

## Cross-Reference Guidelines

### When to Use @see
- Closely related classes
- Alternative implementations
- Required dependencies
- Parent/child classes
- Related interfaces

### Example
```kotlin
/**
 * Vector3 represents a point or direction in 3D space.
 *
 * @see Vector2 For 2D vectors
 * @see Vector4 For 4D vectors
 * @see Matrix4 For transformations
 * @see Quaternion For rotations
 */
```

## Performance Documentation

### When to Include
- Object allocation frequency
- Computational complexity
- Memory usage implications
- Threading considerations
- Platform-specific differences

### Example
```kotlin
/**
 * ## Performance Considerations
 *
 * - Avoid creating new instances in render loop - reuse with [set]
 * - O(1) complexity for component access
 * - Operator overloads allocate new instances - use sparingly
 * - Consider object pooling for frequently allocated vectors
 */
```

## Platform-Specific Notes

### When to Include
- Platform-specific behavior differences
- Platform availability (JVM-only, JS-only)
- Performance characteristics by platform
- Platform-specific limitations

### Example
```kotlin
/**
 * ## Platform Notes
 *
 * - **JVM**: Uses LWJGL for Vulkan rendering
 * - **JavaScript**: Falls back to WebGL2 if WebGPU unavailable
 * - **Native**: Direct Vulkan bindings (MoltenVK on Apple)
 */
```

## Sample Files

Create separate sample files in `src/commonTest/kotlin/io/kreekt/samples/`:

```kotlin
package io.kreekt.samples

import io.kreekt.core.math.Vector3

object Vector3Samples {
    
    fun basicOperations() {
        val v1 = Vector3(1f, 2f, 3f)
        val v2 = Vector3(4f, 5f, 6f)
        
        // Component-wise addition
        val sum = v1 + v2
        println("Sum: $sum")
        
        // Scalar multiplication
        val scaled = v1 * 2f
        println("Scaled: $scaled")
    }
    
    fun transformations() {
        // Example for transformations
    }
}
```

Reference in class documentation:
```kotlin
@sample io.kreekt.samples.Vector3Samples.basicOperations
```

## Quality Checklist

Before committing documentation:

### Class Documentation
- [ ] # Heading with class name present
- [ ] One-line summary in first sentence
- [ ] ## Overview section with bullet points
- [ ] ## Basic Usage with code example
- [ ] All public properties have @property tags
- [ ] @see tags for related classes
- [ ] @sample reference included
- [ ] @since version tag present
- [ ] Performance notes if applicable
- [ ] Platform notes if applicable

### Method Documentation
- [ ] One-line summary present
- [ ] @param for each parameter
- [ ] @return for non-void methods
- [ ] @throws for exceptions
- [ ] Code example for complex methods
- [ ] @see for related methods
- [ ] @since version tag

### Overall Quality
- [ ] No spelling errors
- [ ] Code examples are syntactically correct
- [ ] Examples demonstrate realistic usage
- [ ] Technical terms defined
- [ ] Consistent formatting throughout
- [ ] Cross-references are accurate

## Quick Templates

### Simple Property
```kotlin
/**
 * Brief description of the property.
 *
 * @since 1.0.0
 */
var property: Type = default
```

### Simple Method
```kotlin
/**
 * Brief description of what method does.
 *
 * @param param Description
 * @return Description
 * @since 1.0.0
 */
fun method(param: Type): ReturnType
```

### Data Class
```kotlin
/**
 * # ClassName - Brief Description
 *
 * Details about what this data represents.
 *
 * @property field1 Description of field1
 * @property field2 Description of field2
 * @since 1.0.0
 */
data class ClassName(
    val field1: Type1,
    val field2: Type2
)
```

### Sealed Class
```kotlin
/**
 * # ClassName - Brief Description
 *
 * Sealed class hierarchy representing possible states/types.
 *
 * @see Subclass1
 * @see Subclass2
 * @since 1.0.0
 */
sealed class ClassName {
    /**
     * Brief description of subclass.
     *
     * @property specific Specific property
     */
    data class Subclass1(val specific: Type) : ClassName()
    
    /**
     * Brief description of another subclass.
     */
    object Subclass2 : ClassName()
}
```

## Common Mistakes to Avoid

### ❌ Don't
```kotlin
// Too brief
/** Gets the value */
fun getValue(): Int

// No examples
/** Complex algorithm for X */
fun complexOperation()

// Missing parameters
/** Does something */
fun process(a: Int, b: Int)

// Vague description
/** Returns a thing */
fun getThing(): Thing
```

### ✅ Do
```kotlin
/**
 * Returns the current value of the counter.
 *
 * @return The counter value, guaranteed to be non-negative
 * @since 1.0.0
 */
fun getValue(): Int

/**
 * Applies complex transformation algorithm to input data.
 *
 * Example:
 * ```kotlin
 * val result = complexOperation(data)
 * ```
 *
 * @param data Input data to transform
 * @return Transformed data
 * @since 1.0.0
 */
fun complexOperation(data: Data): Result
```

## References

- [Kotlin KDoc Syntax](https://kotlinlang.org/docs/kotlin-doc.html)
- [Dokka Documentation](https://kotlinlang.org/docs/dokka-introduction.html)
- [Three.js Documentation](https://threejs.org/docs/) - Reference for style
- [Kotlin Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/) - Reference for quality

---

**Version**: 1.0.0
**Last Updated**: 2025-10-02
**Maintained By**: KreeKt Documentation Team
