/**
 * TextGeometry with comprehensive font loading and text rendering support
 * Converts text strings into 3D geometry using vector fonts with advanced typography features
 */
package io.kreekt.geometry

import io.kreekt.core.math.*
import kotlin.math.*
import io.kreekt.core.math.Box3

/**
 * Options for text geometry generation
 */
data class TextOptions(
    val size: Float = 12f,
    val height: Float = 1f,
    val curveSegments: Int = 12,
    val bevelEnabled: Boolean = false,
    val bevelThickness: Float = 0.1f,
    val bevelSize: Float = 0.05f,
    val bevelOffset: Float = 0f,
    val bevelSegments: Int = 3,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.2f,
    val textAlign: TextAlign = TextAlign.LEFT,
    val textBaseline: TextBaseline = TextBaseline.ALPHABETIC,
    val maxWidth: Float? = null,
    val wordWrap: Boolean = false
) {
    init {
        require(size > 0f) { "Text size must be positive" }
        require(height >= 0f) { "Text height must be non-negative" }
        require(curveSegments >= 3) { "Curve segments must be at least 3" }
        require(lineHeight > 0f) { "Line height must be positive" }
        if (bevelEnabled) {
            require(bevelThickness >= 0f) { "Bevel thickness must be non-negative" }
            require(bevelSize >= 0f) { "Bevel size must be non-negative" }
            require(bevelSegments >= 0) { "Bevel segments must be non-negative" }
        }
    }
}

/**
 * Text alignment options
 */
enum class TextAlign {
    LEFT, CENTER, RIGHT, JUSTIFY
}

/**
 * Text baseline options
 */
enum class TextBaseline {
    ALPHABETIC, TOP, HANGING, MIDDLE, IDEOGRAPHIC, BOTTOM
}

/**
 * Font interface for vector font data
 */
interface Font {
    val familyName: String
    val styleName: String
    val unitsPerEm: Int
    val ascender: Float
    val descender: Float
    val lineGap: Float
    val glyphs: Map<Char, Glyph>

    fun getGlyph(char: Char): Glyph?
    fun getKerning(leftChar: Char, rightChar: Char): Float
    fun measureText(text: String, size: Float): TextMetrics
}

/**
 * Glyph data for individual characters
 */
data class Glyph(
    val unicode: Char,
    val width: Float,
    val leftSideBearing: Float,
    val rightSideBearing: Float,
    val path: GlyphPath
)

/**
 * Vector path data for glyph outlines
 */
data class GlyphPath(
    val commands: List<PathCommand>,
    val boundingBox: BoundingBox2D
)

/**
 * Path commands for vector glyph outlines
 */
sealed class PathCommand {
    data class MoveTo(val x: Float, val y: Float) : PathCommand()
    data class LineTo(val x: Float, val y: Float) : PathCommand()
    data class QuadraticCurveTo(val cpx: Float, val cpy: Float, val x: Float, val y: Float) : PathCommand()
    data class BezierCurveTo(val cp1x: Float, val cp1y: Float, val cp2x: Float, val cp2y: Float, val x: Float, val y: Float) : PathCommand()
    object ClosePath : PathCommand()
}

/**
 * Text measurement results
 */
data class TextMetrics(
    val width: Float,
    val height: Float,
    val actualBoundingBoxLeft: Float,
    val actualBoundingBoxRight: Float,
    val actualBoundingBoxAscent: Float,
    val actualBoundingBoxDescent: Float,
    val fontBoundingBoxAscent: Float,
    val fontBoundingBoxDescent: Float
)

/**
 * 2D bounding box for glyph bounds
 */
data class BoundingBox2D(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
}

/**
 * Font loading result
 */
sealed class FontResult<T> {
    data class Success<T>(val value: T) : FontResult<T>()
    data class Error<T>(val exception: Exception) : FontResult<T>()
}

/**
 * Font loader interface for different font formats
 */
interface FontLoader {
    suspend fun loadFont(path: String): FontResult<Font>
    suspend fun loadFont(data: ByteArray): FontResult<Font>
    fun getSupportedFormats(): List<String>
}

/**
 * Text layout information
 */
data class TextLayout(
    val lines: List<TextLine>,
    val totalWidth: Float,
    val totalHeight: Float,
    val baseline: Float
)

/**
 * Individual text line information
 */
data class TextLine(
    val text: String,
    val width: Float,
    val height: Float,
    val offsetX: Float,
    val offsetY: Float,
    val glyphs: List<PositionedGlyph>
)

/**
 * Positioned glyph with transform information
 */
data class PositionedGlyph(
    val glyph: Glyph,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float = 0f
)

/**
 * TextGeometry class for generating 3D text
 */
class TextGeometry(
    val text: String,
    val font: Font,
    val options: TextOptions = TextOptions()
) : BufferGeometry() {

    private val textLayout: TextLayout

    init {
        require(text.isNotEmpty()) { "Text cannot be empty" }
        textLayout = layoutText(text, font, options)
        generate()
    }

    private fun generate() {
        val vertices = mutableListOf<Vector3>()
        val normals = mutableListOf<Vector3>()
        val uvs = mutableListOf<Vector2>()
        val indices = mutableListOf<Int>()

        var vertexOffset = 0

        // Generate geometry for each line
        for (line in textLayout.lines) {
            for (positionedGlyph in line.glyphs) {
                generateGlyphGeometry(
                    positionedGlyph,
                    line.offsetX,
                    line.offsetY,
                    vertices,
                    normals,
                    uvs,
                    indices,
                    vertexOffset
                )
                vertexOffset = vertices.size
            }
        }

        // Set geometry attributes
        setAttribute("position", BufferAttribute(vertices.flatMap { listOf(it.x, it.y, it.z) }.toFloatArray(), 3))
        setAttribute("normal", BufferAttribute(normals.flatMap { listOf(it.x, it.y, it.z) }.toFloatArray(), 3))
        setAttribute("uv", BufferAttribute(uvs.flatMap { listOf(it.x, it.y) }.toFloatArray(), 2))
        setIndex(BufferAttribute(indices.map { it.toFloat() }.toFloatArray(), 1))

        computeBoundingSphere()
    }

    private fun layoutText(text: String, font: Font, options: TextOptions): TextLayout {
        val lines = if (options.wordWrap && options.maxWidth != null) {
            wrapText(text, font, options)
        } else {
            text.split('\n').map { it }
        }

        val textLines = mutableListOf<TextLine>()
        var maxWidth = 0f
        val lineHeight = options.size * options.lineHeight

        for ((lineIndex, lineText) in lines.withIndex()) {
            val line = layoutLine(lineText, font, options)
            val offsetY = -lineIndex * lineHeight

            // Apply text alignment
            val offsetX = when (options.textAlign) {
                TextAlign.LEFT -> 0f
                TextAlign.CENTER -> -line.width / 2f
                TextAlign.RIGHT -> -line.width
                TextAlign.JUSTIFY -> 0f // TODO: Implement justify
            }

            val adjustedLine = line.copy(offsetX = offsetX, offsetY = offsetY)
            textLines.add(adjustedLine)
            maxWidth = max(maxWidth, line.width)
        }

        val totalHeight = textLines.size * lineHeight
        val baseline = calculateBaseline(font, options)

        return TextLayout(textLines, maxWidth, totalHeight, baseline)
    }

    private fun layoutLine(text: String, font: Font, options: TextOptions): TextLine {
        val glyphs = mutableListOf<PositionedGlyph>()
        var currentX = 0f
        val scale = options.size / font.unitsPerEm

        for (i in text.indices) {
            val char = text[i]
            val glyph = font.getGlyph(char) ?: continue

            // Apply kerning if not the first character
            if (i > 0) {
                val kerning = font.getKerning(text[i - 1], char)
                currentX += (kerning * scale)
            }

            val positionedGlyph = PositionedGlyph(
                glyph = glyph,
                x = currentX,
                y = 0f,
                scale = scale
            )

            glyphs.add(positionedGlyph)

            // Advance position
            currentX += (glyph.width + options.letterSpacing) * scale
        }

        val lineWidth = currentX - options.letterSpacing * scale
        val lineHeight = options.size

        return TextLine(text, lineWidth, lineHeight, 0f, 0f, glyphs)
    }

    private fun wrapText(text: String, font: Font, options: TextOptions): List<String> {
        val maxWidth = options.maxWidth!!
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val metrics = font.measureText(testLine, options.size)

            if (metrics.width <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    // Word is too long, break it
                    lines.add(word)
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }

    private fun calculateBaseline(font: Font, options: TextOptions): Float {
        val scale = options.size / font.unitsPerEm

        return when (options.textBaseline) {
            TextBaseline.ALPHABETIC -> 0f
            TextBaseline.TOP -> -font.ascender * scale
            TextBaseline.HANGING -> -font.ascender * scale * 0.8f
            TextBaseline.MIDDLE -> -(font.ascender - font.descender) * scale / 2f
            TextBaseline.IDEOGRAPHIC -> font.descender * scale
            TextBaseline.BOTTOM -> font.descender * scale
        }
    }

    private fun generateGlyphGeometry(
        positionedGlyph: PositionedGlyph,
        lineOffsetX: Float,
        lineOffsetY: Float,
        vertices: MutableList<Vector3>,
        normals: MutableList<Vector3>,
        uvs: MutableList<Vector2>,
        indices: MutableList<Int>,
        vertexOffset: Int
    ) {
        val glyph = positionedGlyph.glyph
        val path = glyph.path
        val transform = TransformMatrix3()
            .scale(positionedGlyph.scale, positionedGlyph.scale)
            .translate(positionedGlyph.x + lineOffsetX, positionedGlyph.y + lineOffsetY)

        // Convert glyph path to 2D shape
        val shapes = convertPathToShapes(path, transform)

        // Generate 3D geometry for each shape
        for (shape in shapes) {
            if (options.height > 0f) {
                // Create extruded geometry
                val extrudeOptions = ExtrudeOptions(
                    depth = options.height,
                    bevelEnabled = options.bevelEnabled,
                    bevelThickness = options.bevelThickness,
                    bevelSize = options.bevelSize,
                    bevelOffset = options.bevelOffset,
                    bevelSegments = options.bevelSegments,
                    steps = 1
                )

                val extrudeGeometry = ExtrudeGeometry(shape, extrudeOptions)
                mergeGeometry(extrudeGeometry, vertices, normals, uvs, indices, vertexOffset)
            } else {
                // Create flat geometry
                generateFlatShapeGeometry(shape, vertices, normals, uvs, indices, vertexOffset)
            }
        }
    }

    private fun convertPathToShapes(path: GlyphPath, transform: TransformMatrix3): List<Shape> {
        val shapes = mutableListOf<Shape>()
        val currentContour = mutableListOf<Vector2>()
        var currentPoint = Vector2()

        for (command in path.commands) {
            when (command) {
                is PathCommand.MoveTo -> {
                    if (currentContour.isNotEmpty()) {
                        shapes.add(SimpleShape(currentContour.toList()))
                        currentContour.clear()
                    }
                    currentPoint = transform.transformPoint(Vector2(command.x, command.y))
                    currentContour.add(currentPoint)
                }

                is PathCommand.LineTo -> {
                    currentPoint = transform.transformPoint(Vector2(command.x, command.y))
                    currentContour.add(currentPoint)
                }

                is PathCommand.QuadraticCurveTo -> {
                    val cp = transform.transformPoint(Vector2(command.cpx, command.cpy))
                    val end = transform.transformPoint(Vector2(command.x, command.y))

                    // Subdivide quadratic curve
                    val curvePoints = subdivideQuadraticCurve(currentPoint, cp, end, options.curveSegments)
                    currentContour.addAll(curvePoints.drop(1)) // Skip first point (already added)
                    currentPoint = end
                }

                is PathCommand.BezierCurveTo -> {
                    val cp1 = transform.transformPoint(Vector2(command.cp1x, command.cp1y))
                    val cp2 = transform.transformPoint(Vector2(command.cp2x, command.cp2y))
                    val end = transform.transformPoint(Vector2(command.x, command.y))

                    // Subdivide bezier curve
                    val curvePoints = subdivideBezierCurve(currentPoint, cp1, cp2, end, options.curveSegments)
                    currentContour.addAll(curvePoints.drop(1)) // Skip first point (already added)
                    currentPoint = end
                }

                is PathCommand.ClosePath -> {
                    if (currentContour.isNotEmpty()) {
                        shapes.add(SimpleShape(currentContour.toList()))
                        currentContour.clear()
                    }
                }
            }
        }

        if (currentContour.isNotEmpty()) {
            shapes.add(SimpleShape(currentContour.toList()))
        }

        return shapes
    }

    private fun subdivideQuadraticCurve(start: Vector2, control: Vector2, end: Vector2, segments: Int): List<Vector2> {
        val points = mutableListOf<Vector2>()

        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val point = quadraticBezier(start, control, end, t)
            points.add(point)
        }

        return points
    }

    private fun subdivideBezierCurve(start: Vector2, cp1: Vector2, cp2: Vector2, end: Vector2, segments: Int): List<Vector2> {
        val points = mutableListOf<Vector2>()

        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val point = cubicBezier(start, cp1, cp2, end, t)
            points.add(point)
        }

        return points
    }

    private fun quadraticBezier(p0: Vector2, p1: Vector2, p2: Vector2, t: Float): Vector2 {
        val invT = 1f - t
        return Vector2(
            invT * invT * p0.x + 2f * invT * t * p1.x + t * t * p2.x,
            invT * invT * p0.y + 2f * invT * t * p1.y + t * t * p2.y
        )
    }

    private fun cubicBezier(p0: Vector2, p1: Vector2, p2: Vector2, p3: Vector2, t: Float): Vector2 {
        val invT = 1f - t
        val invT2 = invT * invT
        val invT3 = invT2 * invT
        val t2 = t * t
        val t3 = t2 * t

        return Vector2(
            invT3 * p0.x + 3f * invT2 * t * p1.x + 3f * invT * t2 * p2.x + t3 * p3.x,
            invT3 * p0.y + 3f * invT2 * t * p1.y + 3f * invT * t2 * p2.y + t3 * p3.y
        )
    }

    private fun generateFlatShapeGeometry(
        shape: Shape,
        vertices: MutableList<Vector3>,
        normals: MutableList<Vector3>,
        uvs: MutableList<Vector2>,
        indices: MutableList<Int>,
        vertexOffset: Int
    ) {
        // Triangulate the shape and create flat geometry
        val triangles = triangulateShape(shape.points, shape.holes.map { it.points })
        val startVertexIndex = vertices.size

        // Add vertices
        for (point in shape.points) {
            vertices.add(Vector3(point.x, point.y, 0f))
            normals.add(Vector3(0f, 0f, 1f))
            uvs.add(Vector2(point.x, point.y)) // Simple UV mapping
        }

        // Add triangles
        for (triangle in triangles) {
            indices.addAll(triangle.map { it + startVertexIndex })
        }
    }

    private fun triangulateShape(shapePoints: List<Vector2>, holes: List<List<Vector2>>): List<List<Int>> {
        // Simple triangulation - in practice, use a robust library like earcut
        val points = shapePoints.toMutableList()
        val triangles = mutableListOf<List<Int>>()

        // Add hole vertices (simplified implementation)
        for (hole in holes) {
            points.addAll(hole)
        }

        // Simple fan triangulation (works for convex shapes)
        for (i in 1 until points.size - 1) {
            triangles.add(listOf(0, i, i + 1))
        }

        return triangles
    }

    private fun mergeGeometry(
        sourceGeometry: BufferGeometry,
        targetVertices: MutableList<Vector3>,
        targetNormals: MutableList<Vector3>,
        targetUVs: MutableList<Vector2>,
        targetIndices: MutableList<Int>,
        vertexOffset: Int
    ) {
        // Extract vertices from source geometry
        val positionAttribute = sourceGeometry.getAttribute("position")!!
        val normalAttribute = sourceGeometry.getAttribute("normal")!!
        val uvAttribute = sourceGeometry.getAttribute("uv")!!
        val indexAttribute = sourceGeometry.index!!

        val startVertexIndex = targetVertices.size

        // Add vertices
        for (i in 0 until positionAttribute.count) {
            val x = positionAttribute.getX(i)
            val y = positionAttribute.getY(i)
            val z = positionAttribute.getZ(i)
            targetVertices.add(Vector3(x, y, z))
        }

        // Add normals
        for (i in 0 until normalAttribute.count) {
            val x = normalAttribute.getX(i)
            val y = normalAttribute.getY(i)
            val z = normalAttribute.getZ(i)
            targetNormals.add(Vector3(x, y, z))
        }

        // Add UVs
        for (i in 0 until uvAttribute.count) {
            val x = uvAttribute.getX(i)
            val y = uvAttribute.getY(i)
            targetUVs.add(Vector2(x, y))
        }

        // Add indices
        for (i in 0 until indexAttribute.count) {
            targetIndices.add(indexAttribute.getX(i).toInt() + startVertexIndex)
        }
    }

    /**
     * Get text layout information
     */
    fun getTextLayout(): TextLayout = textLayout

    /**
     * Get text bounds in 3D space
     */
    fun getTextBounds(): Box3 {
        return computeBoundingBox()
    }
}

/**
 * Simple font implementation for testing
 */
class SimpleFont(
    override val familyName: String,
    override val styleName: String = "Regular",
    override val unitsPerEm: Int = 1000,
    override val ascender: Float = 800f,
    override val descender: Float = -200f,
    override val lineGap: Float = 100f,
    override val glyphs: Map<Char, Glyph> = emptyMap()
) : Font {

    override fun getGlyph(char: Char): Glyph? = glyphs[char]

    override fun getKerning(leftChar: Char, rightChar: Char): Float = 0f

    override fun measureText(text: String, size: Float): TextMetrics {
        val scale = size / unitsPerEm
        var width = 0f

        for (char in text) {
            val glyph = getGlyph(char)
            if (glyph != null) {
                width = width + glyph.width * scale
            }
        }

        return TextMetrics(
            width = width,
            height = size,
            actualBoundingBoxLeft = 0f,
            actualBoundingBoxRight = width,
            actualBoundingBoxAscent = (ascender * scale),
            actualBoundingBoxDescent = -(descender * scale),
            fontBoundingBoxAscent = (ascender * scale),
            fontBoundingBoxDescent = -(descender * scale)
        )
    }
}

/**
 * Helper class for 3x3 matrix transformations
 */
class TransformMatrix3 {
    private val elements = FloatArray(9) { 0f }

    init {
        identity()
    }

    fun identity(): TransformMatrix3 {
        elements[0] = 1f; elements[1] = 0f; elements[2] = 0f
        elements[3] = 0f; elements[4] = 1f; elements[5] = 0f
        elements[6] = 0f; elements[7] = 0f; elements[8] = 1f
        return this
    }

    fun scale(sx: Float, sy: Float): TransformMatrix3 {
        elements[0] *= sx; elements[3] *= sx; elements[6] *= sx
        elements[1] *= sy; elements[4] *= sy; elements[7] *= sy
        return this
    }

    fun translate(tx: Float, ty: Float): TransformMatrix3 {
        elements[6] += elements[0] * tx + elements[3] * ty
        elements[7] += elements[1] * tx + elements[4] * ty
        return this
    }

    fun transformPoint(point: Vector2): Vector2 {
        val x = point.x
        val y = point.y
        return Vector2(
            elements[0] * x + elements[3] * y + elements[6],
            elements[1] * x + elements[4] * y + elements[7]
        )
    }
}

/**
 * Utility functions for text geometry
 */
object TextGeometryHelper {

    /**
     * Create a simple rectangular font for testing
     */
    fun createTestFont(): Font {
        val glyphs = mutableMapOf<Char, Glyph>()

        // Simple rectangular glyphs for basic ASCII characters
        for (c in 'A'..'Z') {
            val path = GlyphPath(
                commands = listOf(
                    PathCommand.MoveTo(0f, 0f),
                    PathCommand.LineTo(500f, 0f),
                    PathCommand.LineTo(500f, 700f),
                    PathCommand.LineTo(0f, 700f),
                    PathCommand.ClosePath
                ),
                boundingBox = BoundingBox2D(0f, 0f, 500f, 700f)
            )

            glyphs[c] = Glyph(
                unicode = c,
                width = 600f,
                leftSideBearing = 50f,
                rightSideBearing = 50f,
                path = path
            )
        }

        return SimpleFont("TestFont", "Regular", 1000, 800f, -200f, 100f, glyphs)
    }

    /**
     * Create text geometry with default settings
     */
    fun createText(text: String, font: Font? = null): TextGeometry {
        val usedFont = font ?: createTestFont()
        return TextGeometry(text, usedFont, TextOptions())
    }
}