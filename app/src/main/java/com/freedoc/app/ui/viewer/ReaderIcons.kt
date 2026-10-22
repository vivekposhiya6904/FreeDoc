package com.freedoc.app.ui.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Clean, lightweight, self-contained vector icons tailored for the FreeDoc PDF reader.
 * Uses no external icon dependencies and scales crisply across all display densities.
 */
object ReaderIcons {

    /**
     * Compact 'X' close icon.
     */
    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }.build()
    }

    /**
     * Upward/Previous navigation chevron.
     */
    val PreviousPage: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderPreviousPage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 14f)
                lineTo(12f, 9f)
                lineTo(17f, 14f)
            }
        }.build()
    }

    /**
     * Downward/Next navigation chevron.
     */
    val NextPage: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderNextPage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 10f)
                lineTo(12f, 15f)
                lineTo(17f, 10f)
            }
        }.build()
    }

    /**
     * Direct page jump / hashtag symbol.
     */
    val JumpToPage: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderJumpToPage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Vertical bars
                moveTo(9.5f, 4.5f)
                lineTo(7.5f, 19.5f)
                moveTo(16.5f, 4.5f)
                lineTo(14.5f, 19.5f)
                // Horizontal bars
                moveTo(5f, 9f)
                lineTo(19.5f, 9f)
                moveTo(4.5f, 15f)
                lineTo(19f, 15f)
            }
        }.build()
    }

    /**
     * Folder / file selection icon.
     */
    val OpenFile: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderOpenFile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 7f)
                lineTo(9f, 7f)
                lineTo(11f, 9f)
                lineTo(21f, 9f)
                lineTo(21f, 18f)
                lineTo(3f, 18f)
                close()
            }
        }.build()
    }

    /**
     * Magnifying glass zoom in (+) icon.
     */
    val ZoomIn: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderZoomIn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Outer circle
                moveTo(16f, 11f)
                curveTo(16f, 13.76f, 13.76f, 16f, 11f, 16f)
                curveTo(8.24f, 16f, 6f, 13.76f, 6f, 11f)
                curveTo(6f, 8.24f, 8.24f, 6f, 11f, 6f)
                curveTo(13.76f, 6f, 16f, 8.24f, 16f, 11f)
                // Handle
                moveTo(15f, 15f)
                lineTo(20f, 20f)
                // Plus sign
                moveTo(8.5f, 11f)
                lineTo(13.5f, 11f)
                moveTo(11f, 8.5f)
                lineTo(11f, 13.5f)
            }
        }.build()
    }

    /**
     * Magnifying glass zoom out (-) icon.
     */
    val ZoomOut: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderZoomOut",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Outer circle
                moveTo(16f, 11f)
                curveTo(16f, 13.76f, 13.76f, 16f, 11f, 16f)
                curveTo(8.24f, 16f, 6f, 13.76f, 6f, 11f)
                curveTo(6f, 8.24f, 8.24f, 6f, 11f, 6f)
                curveTo(13.76f, 6f, 16f, 8.24f, 16f, 11f)
                // Handle
                moveTo(15f, 15f)
                lineTo(20f, 20f)
                // Minus sign
                moveTo(8.5f, 11f)
                lineTo(13.5f, 11f)
            }
        }.build()
    }

    /**
     * Reset zoom / Fit page icon.
     */
    val ResetZoom: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderResetZoom",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Corner brackets
                moveTo(4f, 9f)
                lineTo(4f, 4f)
                lineTo(9f, 4f)

                moveTo(20f, 9f)
                lineTo(20f, 4f)
                lineTo(15f, 4f)

                moveTo(4f, 15f)
                lineTo(4f, 20f)
                lineTo(9f, 20f)

                moveTo(20f, 15f)
                lineTo(20f, 20f)
                lineTo(15f, 20f)

                // Center dot
                moveTo(12f, 11f)
                lineTo(12f, 13f)
            }
        }.build()
    }

    /**
     * Hide controls / Enter immersive reading mode icon.
     */
    val HideControls: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderHideControls",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 9f)
                lineTo(12f, 16f)
                lineTo(19f, 9f)
            }
        }.build()
    }

    /**
     * Show controls / Settings toggle icon.
     */
    val ShowControls: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderShowControls",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(4f, 18f)
                lineTo(20f, 18f)
            }
        }.build()
    }

    /**
     * Document text search icon (magnifying glass).
     */
    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Circle
                moveTo(16f, 10.5f)
                curveTo(16f, 13.54f, 13.54f, 16f, 10.5f, 16f)
                curveTo(7.46f, 16f, 5f, 13.54f, 5f, 10.5f)
                curveTo(5f, 7.46f, 7.46f, 5f, 10.5f, 5f)
                curveTo(13.54f, 5f, 16f, 7.46f, 16f, 10.5f)
                // Handle
                moveTo(14.5f, 14.5f)
                lineTo(19.5f, 19.5f)
            }
        }.build()
    }

    /**
     * Search previous match (up chevron).
     */
    val SearchUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderSearchUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 14f)
                lineTo(12f, 8f)
                lineTo(18f, 14f)
            }
        }.build()
    }

    /**
     * Search next match (down chevron).
     */
    val SearchDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderSearchDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 10f)
                lineTo(12f, 16f)
                lineTo(18f, 10f)
            }
        }.build()
    }

    /**
     * Clear search query 'x' icon.
     */
    val SearchClear: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderSearchClear",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5f, 5f)
                lineTo(15f, 15f)
                moveTo(15f, 5f)
                lineTo(5f, 15f)
            }
        }.build()
    }

    /**
     * Copy selection icon.
     */
    val ContentCopy: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReaderContentCopy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Secondary background page
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 4f)
                lineTo(19f, 4f)
                lineTo(19f, 15f)
            }
            // Primary foreground page
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 8f)
                lineTo(15f, 8f)
                lineTo(15f, 20f)
                lineTo(5f, 20f)
                close()
            }
        }.build()
    }
}
