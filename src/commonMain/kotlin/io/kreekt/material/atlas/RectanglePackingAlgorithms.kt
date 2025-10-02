/**
 * Rectangle packing algorithms for texture atlas generation
 * T028 - Extracted from TextureAtlas.kt for Single Responsibility Principle
 */
package io.kreekt.material.atlas

import io.kreekt.material.Rectangle

/**
 * Base interface for rectangle packing algorithms
 */
interface RectanglePacker {
    fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle?
    fun markRectangleAsUsed(rectangle: Rectangle)
    fun freeRectangle(rectangle: Rectangle)
    fun reset()
}

/**
 * Max Rects packing algorithm implementation
 * Best suited for general-purpose texture atlasing
 */
class MaxRectsPackager : RectanglePacker {
    private val freeRectangles = mutableListOf<Rectangle>()
    private val usedRectangles = mutableListOf<Rectangle>()

    init {
        reset()
    }

    override fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle? {
        var bestRectangle: Rectangle? = null
        var bestShortSideFit = Int.MAX_VALUE
        var bestLongSideFit = Int.MAX_VALUE

        for (rect in freeRectangles) {
            // Try normal orientation
            if (rect.width >= width && rect.height >= height) {
                val leftoverHorizontal = rect.width - width
                val leftoverVertical = rect.height - height
                val shortSideFit = minOf(leftoverHorizontal, leftoverVertical)
                val longSideFit = maxOf(leftoverHorizontal, leftoverVertical)

                if (shortSideFit < bestShortSideFit ||
                    (shortSideFit == bestShortSideFit && longSideFit < bestLongSideFit)) {
                    bestRectangle = Rectangle(rect.x, rect.y, width, height, false)
                    bestShortSideFit = shortSideFit
                    bestLongSideFit = longSideFit
                }
            }

            // Try rotated orientation
            if (allowRotation && rect.width >= height && rect.height >= width) {
                val leftoverHorizontal = rect.width - height
                val leftoverVertical = rect.height - width
                val shortSideFit = minOf(leftoverHorizontal, leftoverVertical)
                val longSideFit = maxOf(leftoverHorizontal, leftoverVertical)

                if (shortSideFit < bestShortSideFit ||
                    (shortSideFit == bestShortSideFit && longSideFit < bestLongSideFit)) {
                    bestRectangle = Rectangle(rect.x, rect.y, height, width, true)
                    bestShortSideFit = shortSideFit
                    bestLongSideFit = longSideFit
                }
            }
        }

        return bestRectangle
    }

    override fun markRectangleAsUsed(rectangle: Rectangle) {
        usedRectangles.add(rectangle)

        // Split intersecting free rectangles
        val toRemove = mutableListOf<Rectangle>()
        val toAdd = mutableListOf<Rectangle>()

        for (freeRect in freeRectangles) {
            if (rectanglesIntersect(rectangle, freeRect)) {
                toRemove.add(freeRect)
                // Create new rectangles from the split
                val splitRects = splitRectangle(freeRect, rectangle)
                toAdd.addAll(splitRects)
            }
        }

        freeRectangles.removeAll(toRemove)
        freeRectangles.addAll(toAdd)

        // Remove redundant rectangles
        pruneRectangles()
    }

    override fun freeRectangle(rectangle: Rectangle) {
        usedRectangles.remove(rectangle)
        freeRectangles.add(rectangle)
        // Merge adjacent free rectangles
        mergeRectangles()
    }

    override fun reset() {
        freeRectangles.clear()
        usedRectangles.clear()
        freeRectangles.add(Rectangle(0, 0, 2048, 2048)) // Default atlas size
    }

    private fun rectanglesIntersect(a: Rectangle, b: Rectangle): Boolean {
        return !(a.x >= b.x + b.width || a.x + a.width <= b.x ||
                 a.y >= b.y + b.height || a.y + a.height <= b.y)
    }

    private fun splitRectangle(freeRect: Rectangle, usedRect: Rectangle): List<Rectangle> {
        val result = mutableListOf<Rectangle>()

        // Left side
        if (usedRect.x > freeRect.x && usedRect.x < freeRect.x + freeRect.width) {
            result.add(Rectangle(
                freeRect.x, freeRect.y,
                usedRect.x - freeRect.x, freeRect.height
            ))
        }

        // Right side
        if (usedRect.x + usedRect.width < freeRect.x + freeRect.width) {
            result.add(Rectangle(
                usedRect.x + usedRect.width, freeRect.y,
                freeRect.x + freeRect.width - (usedRect.x + usedRect.width), freeRect.height
            ))
        }

        // Bottom side
        if (usedRect.y > freeRect.y && usedRect.y < freeRect.y + freeRect.height) {
            result.add(Rectangle(
                freeRect.x, freeRect.y,
                freeRect.width, usedRect.y - freeRect.y
            ))
        }

        // Top side
        if (usedRect.y + usedRect.height < freeRect.y + freeRect.height) {
            result.add(Rectangle(
                freeRect.x, usedRect.y + usedRect.height,
                freeRect.width, freeRect.y + freeRect.height - (usedRect.y + usedRect.height)
            ))
        }

        return result
    }

    private fun pruneRectangles() {
        val toRemove = mutableListOf<Rectangle>()
        for (i in freeRectangles.indices) {
            for (j in freeRectangles.indices) {
                if (i != j && rectangleContains(freeRectangles[j], freeRectangles[i])) {
                    toRemove.add(freeRectangles[i])
                    break
                }
            }
        }
        freeRectangles.removeAll(toRemove)
    }

    private fun rectangleContains(container: Rectangle, contained: Rectangle): Boolean {
        return container.x <= contained.x &&
               container.y <= contained.y &&
               container.x + container.width >= contained.x + contained.width &&
               container.y + container.height >= contained.y + contained.height
    }

    private fun mergeRectangles() {
        var merged = true
        while (merged) {
            merged = false
            for (i in freeRectangles.indices) {
                for (j in i + 1 until freeRectangles.size) {
                    val rect1 = freeRectangles[i]
                    val rect2 = freeRectangles[j]
                    val mergedRect = tryMergeRectangles(rect1, rect2)
                    if (mergedRect != null) {
                        freeRectangles.removeAt(j)
                        freeRectangles.removeAt(i)
                        freeRectangles.add(mergedRect)
                        merged = true
                        break
                    }
                }
                if (merged) break
            }
        }
    }

    private fun tryMergeRectangles(a: Rectangle, b: Rectangle): Rectangle? {
        // Can merge horizontally
        if (a.y == b.y && a.height == b.height) {
            if (a.x + a.width == b.x) {
                return Rectangle(a.x, a.y, a.width + b.width, a.height)
            }
            if (b.x + b.width == a.x) {
                return Rectangle(b.x, b.y, a.width + b.width, a.height)
            }
        }

        // Can merge vertically
        if (a.x == b.x && a.width == b.width) {
            if (a.y + a.height == b.y) {
                return Rectangle(a.x, a.y, a.width, a.height + b.height)
            }
            if (b.y + b.height == a.y) {
                return Rectangle(a.x, b.y, a.width, a.height + b.height)
            }
        }

        return null
    }
}

/**
 * Skyline packing algorithm
 * Optimized for long horizontal textures
 */
class SkylinePackager : RectanglePacker {
    private data class SkylineNode(var x: Int, var y: Int, var width: Int)
    private val skyline = mutableListOf(SkylineNode(0, 0, Int.MAX_VALUE))

    override fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle? {
        var bestY = Int.MAX_VALUE
        var bestIndex = -1
        var bestX = 0

        for (i in skyline.indices) {
            val node = skyline[i]
            if (node.width >= width) {
                val y = node.y
                if (y < bestY) {
                    bestY = y
                    bestX = node.x
                    bestIndex = i
                }
            }
        }

        return if (bestIndex != -1) {
            Rectangle(bestX, bestY, width, height)
        } else null
    }

    override fun markRectangleAsUsed(rectangle: Rectangle) {
        // Update skyline with new rectangle
        val newNode = SkylineNode(rectangle.x, rectangle.y + rectangle.height, rectangle.width)
        skyline.add(newNode)
        skyline.sortBy { it.x }
    }

    override fun freeRectangle(rectangle: Rectangle) {
        // Not typically supported in skyline algorithm
    }

    override fun reset() {
        skyline.clear()
        skyline.add(SkylineNode(0, 0, Int.MAX_VALUE))
    }
}

/**
 * Guillotine packing algorithm
 * Fast but less optimal than MaxRects
 */
class GuillotinePackager : RectanglePacker {
    private val freeRects = mutableListOf<Rectangle>()

    init {
        reset()
    }

    override fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle? {
        var bestRect: Rectangle? = null
        var bestScore = Int.MAX_VALUE

        for (rect in freeRects) {
            if (rect.width >= width && rect.height >= height) {
                val leftoverX = rect.width - width
                val leftoverY = rect.height - height
                val score = minOf(leftoverX, leftoverY)

                if (score < bestScore) {
                    bestScore = score
                    bestRect = Rectangle(rect.x, rect.y, width, height)
                }
            }
        }

        return bestRect
    }

    override fun markRectangleAsUsed(rectangle: Rectangle) {
        // Split free rectangles using guillotine cuts
        val toRemove = mutableListOf<Rectangle>()
        val toAdd = mutableListOf<Rectangle>()

        for (rect in freeRects) {
            if (intersects(rect, rectangle)) {
                toRemove.add(rect)

                // Horizontal split
                if (rect.x < rectangle.x) {
                    toAdd.add(Rectangle(rect.x, rect.y, rectangle.x - rect.x, rect.height))
                }
                if (rectangle.x + rectangle.width < rect.x + rect.width) {
                    toAdd.add(Rectangle(
                        rectangle.x + rectangle.width,
                        rect.y,
                        rect.x + rect.width - rectangle.x - rectangle.width,
                        rect.height
                    ))
                }

                // Vertical split
                if (rect.y < rectangle.y) {
                    toAdd.add(Rectangle(rect.x, rect.y, rect.width, rectangle.y - rect.y))
                }
                if (rectangle.y + rectangle.height < rect.y + rect.height) {
                    toAdd.add(Rectangle(
                        rect.x,
                        rectangle.y + rectangle.height,
                        rect.width,
                        rect.y + rect.height - rectangle.y - rectangle.height
                    ))
                }
            }
        }

        freeRects.removeAll(toRemove)
        freeRects.addAll(toAdd)
    }

    override fun freeRectangle(rectangle: Rectangle) {
        freeRects.add(rectangle)
    }

    override fun reset() {
        freeRects.clear()
        freeRects.add(Rectangle(0, 0, 4096, 4096))
    }

    private fun intersects(a: Rectangle, b: Rectangle): Boolean {
        return !(a.x >= b.x + b.width || a.x + a.width <= b.x ||
                 a.y >= b.y + b.height || a.y + a.height <= b.y)
    }
}

/**
 * Bottom-Left packing algorithm
 * Simple but fast for specific use cases
 */
class BottomLeftPackager : RectanglePacker {
    override fun findBestFit(width: Int, height: Int, allowRotation: Boolean): Rectangle? = null
    override fun markRectangleAsUsed(rectangle: Rectangle) {}
    override fun freeRectangle(rectangle: Rectangle) {}
    override fun reset() {}
}
