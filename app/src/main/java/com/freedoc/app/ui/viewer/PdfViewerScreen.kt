package com.freedoc.app.ui.viewer

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.freedoc.app.engine.pdf.PageDimensions
import com.freedoc.app.engine.pdf.PdfSelection
import com.freedoc.app.engine.pdf.PdfTextExtractor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedoc.app.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Neutral Grey Reader Design Palette
private val ReaderGreyBg = Color(0xF21C1E22)
private val ReaderGreyElevated = Color(0xFF282B30)
private val ReaderGreyBorder = Color(0xFF3B3F46)
private val ReaderTextPrimary = Color(0xFFF3F4F6)
private val ReaderTextSecondary = Color(0xFF9CA3AF)
private val ReaderScrubberTrack = Color(0x2EFFFFFF)
private val ReaderScrubberThumb = Color(0xFF7E848D)
private val ReaderScrubberThumbActive = Color(0xFFBCC1C8)
private val ReaderAccentGrey = Color(0xFF4B515B)

/**
 * Compose screen for rendering and interacting with PDF documents via continuous vertical scrolling.
 * Features an immersive reading mode with floating overlays, a right-side fast-scroll scrubber,
 * compact icon controls, text search, and reliable direct page navigation.
 */
@Composable
fun PdfViewerScreen(
    uiState: PdfViewerUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSelectAnotherFile: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    navigationRequest: NavigationRequest? = null,
    onPageVisible: (Int) -> Unit = {},
    onRequestPage: (Int) -> Unit = {},
    onJumpToPage: (Int) -> Unit = {},
    searchState: SearchUiState = SearchUiState(),
    onOpenSearch: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onNextSearchMatch: () -> Unit = {},
    onPreviousSearchMatch: () -> Unit = {},
    selection: PdfSelection? = null,
    onSelectWordAt: (pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit = { _, _, _ -> },
    onExtendSelectionTo: (pageIndex: Int, pdfX: Float, pdfY: Float, isStartHandle: Boolean) -> Unit = { _, _, _, _ -> },
    onStartSelectionDrag: (isStartHandle: Boolean) -> Unit = {},
    onEndSelectionDrag: () -> Unit = {},
    onSelectAllOnPage: (pageIndex: Int) -> Unit = {},
    onClearSelection: () -> Unit = {},
    getPageContent: (Int) -> PdfTextExtractor.PageTextContent? = { null }
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.statusBars())

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    var showJumpDialog by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(false) }
    var directJumpTrigger by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E2124))
    ) {
        when (uiState) {
            is PdfViewerUiState.Idle -> {
                EmptyViewerState(
                    onSelectFile = onSelectAnotherFile,
                    onClose = onClose
                )
            }
            is PdfViewerUiState.Loading -> {
                LoadingViewerState(
                    message = uiState.message,
                    onClose = onClose
                )
            }
            is PdfViewerUiState.Ready -> {
                ContinuousPdfPageView(
                    state = uiState,
                    isControlsVisible = isControlsVisible,
                    searchState = searchState,
                    navigationRequest = navigationRequest,
                    directJumpTarget = directJumpTrigger,
                    onDirectJumpHandled = { directJumpTrigger = null },
                    onToggleControls = { isControlsVisible = !isControlsVisible },
                    onOpenJumpDialog = { showJumpDialog = true },
                    onPageVisible = onPageVisible,
                    onRequestPage = onRequestPage,
                    onJumpToPage = onJumpToPage,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage,
                    onSelectAnotherFile = onSelectAnotherFile,
                    onClose = onClose,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onNextSearchMatch = onNextSearchMatch,
                    onPreviousSearchMatch = onPreviousSearchMatch,
                    selection = selection,
                    onSelectWordAt = onSelectWordAt,
                    onExtendSelectionTo = onExtendSelectionTo,
                    onStartSelectionDrag = onStartSelectionDrag,
                    onEndSelectionDrag = onEndSelectionDrag,
                    onSelectAllOnPage = onSelectAllOnPage,
                    onClearSelection = onClearSelection,
                    getPageContent = getPageContent,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is PdfViewerUiState.Error -> {
                ErrorViewerState(
                    state = uiState,
                    onSelectAnotherFile = onSelectAnotherFile,
                    onClose = onClose
                )
            }
        }

        if (showJumpDialog && uiState is PdfViewerUiState.Ready) {
            JumpToPageDialog(
                currentPageOneBased = uiState.displayPageNumber,
                totalPages = uiState.totalPages,
                onDismiss = { showJumpDialog = false },
                onConfirmJump = { targetPageOneBased ->
                    directJumpTrigger = targetPageOneBased - 1
                    onJumpToPage(targetPageOneBased)
                    showJumpDialog = false
                }
            )
        }
    }
}

/**
 * Continuous vertical scrolling layout rendering pages in a LazyColumn with pinch-to-zoom,
 * right-side draggable fast-scroll scrubber, and compact neutral-grey floating controls.
 */
@Composable
private fun ContinuousPdfPageView(
    state: PdfViewerUiState.Ready,
    isControlsVisible: Boolean,
    searchState: SearchUiState,
    navigationRequest: NavigationRequest? = null,
    directJumpTarget: Int? = null,
    onDirectJumpHandled: () -> Unit = {},
    onToggleControls: () -> Unit,
    onOpenJumpDialog: () -> Unit,
    onPageVisible: (Int) -> Unit,
    onRequestPage: (Int) -> Unit,
    onJumpToPage: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSelectAnotherFile: () -> Unit,
    onClose: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onNextSearchMatch: () -> Unit,
    onPreviousSearchMatch: () -> Unit,
    selection: PdfSelection? = null,
    onSelectWordAt: (pageIndex: Int, pdfX: Float, pdfY: Float) -> Unit = { _, _, _ -> },
    onExtendSelectionTo: (pageIndex: Int, pdfX: Float, pdfY: Float, isStartHandle: Boolean) -> Unit = { _, _, _, _ -> },
    onStartSelectionDrag: (isStartHandle: Boolean) -> Unit = {},
    onEndSelectionDrag: () -> Unit = {},
    onSelectAllOnPage: (pageIndex: Int) -> Unit = {},
    onClearSelection: () -> Unit = {},
    getPageContent: (Int) -> PdfTextExtractor.PageTextContent? = { null },
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentPage)
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Mutex flag preventing feedback loops between user gestures and programmatic jumps
    var isProgrammaticScrolling by remember { mutableStateOf(false) }

    // Synchronize visible page during continuous scroll based on item closest to viewport center
    val currentVisibleItem by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                listState.firstVisibleItemIndex.coerceIn(0, (state.totalPages - 1).coerceAtLeast(0))
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val centerItem = visibleItems.minByOrNull {
                    val itemCenter = it.offset + (it.size / 2)
                    abs(itemCenter - viewportCenter)
                }
                (centerItem?.index ?: listState.firstVisibleItemIndex).coerceIn(0, state.totalPages - 1)
            }
        }
    }

    // Report viewport changes to ViewModel only during natural scrolling
    LaunchedEffect(currentVisibleItem) {
        if (!isProgrammaticScrolling && currentVisibleItem in 0 until state.totalPages) {
            onPageVisible(currentVisibleItem)
        }
    }

    // Helper for fast, race-condition free programmatic jumps with optional scroll offset
    val performPageJumpWithOffset: (Int, Int, Boolean, Boolean) -> Unit = { targetIndexZeroBased, scrollOffsetPx, animate, commitRender ->
        val safeIndex = targetIndexZeroBased.coerceIn(0, state.totalPages - 1)
        coroutineScope.launch {
            isProgrammaticScrolling = true
            try {
                if (animate) {
                    listState.animateScrollToItem(safeIndex, scrollOffsetPx)
                } else {
                    listState.scrollToItem(safeIndex, scrollOffsetPx)
                }
                if (commitRender) {
                    onPageVisible(safeIndex)
                }
            } finally {
                isProgrammaticScrolling = false
            }
        }
    }

    val performPageJump: (Int, Boolean, Boolean) -> Unit = { targetIndexZeroBased, animate, commitRender ->
        performPageJumpWithOffset(targetIndexZeroBased, 0, animate, commitRender)
    }

    // Handle direct navigation jump trigger
    LaunchedEffect(directJumpTarget) {
        directJumpTarget?.let { target ->
            performPageJump(target, false, true)
            onDirectJumpHandled()
        }
    }

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (selection != null) {
                    onClearSelection()
                } else {
                    onToggleControls()
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var accumulatedPan = Offset.Zero
                    var hasExceededSlop = false
                    val touchSlop = 16f
                    do {
                        val event = awaitPointerEvent()
                        // If any pointer change was already consumed by a child (e.g. handle drag), do not pan or zoom
                        if (event.changes.any { it.isConsumed }) {
                            continue
                        }

                        val pointerCount = event.changes.size
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        // Multi-touch pinch-to-zoom
                        if (pointerCount >= 2 && kotlin.math.abs(zoom - 1f) > 0.005f) {
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            if (newScale > 1.05f) {
                                val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x * newScale).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                                event.changes.forEach { it.consume() }
                            } else {
                                offset = Offset.Zero
                            }
                        } else if (scale > 1.05f && pointerCount == 1) {
                            // Zoomed-in single finger panning requires exceeding touch slop first
                            // to avoid stealing long-press word selection gestures!
                            accumulatedPan += pan
                            if (!hasExceededSlop && accumulatedPan.getDistance() > touchSlop) {
                                hasExceededSlop = true
                            }
                            if (hasExceededSlop && (abs(pan.x) > 0.1f || abs(pan.y) > 0.1f)) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        // Handle programmatic navigation request with exact match centering and controlled zoom-to-selection
        LaunchedEffect(navigationRequest?.requestId) {
            navigationRequest?.let { req ->
                val safeIndex = req.pageIndex.coerceIn(0, (state.totalPages - 1).coerceAtLeast(0))
                val targetMatch = if (req.matchIndex >= 0) searchState.matches.getOrNull(req.matchIndex) else null
                val targetRect = targetMatch?.highlightRects?.firstOrNull()
                val pageDims = state.pageDimensions[safeIndex]
                val pageWidth = pageDims?.width?.toFloat() ?: 595f
                val pageHeight = pageDims?.height?.toFloat() ?: 842f
                val pageAspectRatio = pageDims?.aspectRatio ?: (pageWidth / pageHeight)

                val viewportWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val viewportHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val horizontalPaddingPx = with(density) { 40.dp.toPx() }
                val contentWidthPx = (viewportWidthPx - horizontalPaddingPx).coerceAtLeast(100f)
                val renderedPageHeightPx = contentWidthPx / pageAspectRatio

                // 1. Calculate scroll offset so the selected word is positioned at comfortable 30% viewport height
                // This ensures it is never hidden behind the top system bars or bottom search toolbar
                val scrollOffsetPx = if (targetRect != null && pageHeight > 0) {
                    val matchYInPage = (targetRect.top / pageHeight) * renderedPageHeightPx
                    val desiredScreenY = viewportHeightPx * 0.30f
                    (matchYInPage - desiredScreenY).toInt().coerceAtLeast(0)
                } else {
                    0
                }

                // 2. Controlled zoom-to-selection: gently ensure text is comfortably readable (>= 1.45x)
                // If the user already zoomed in further, preserve their zoom level!
                val minComfortableScale = 1.45f
                val targetScale = if (req.matchIndex >= 0) {
                    if (scale < minComfortableScale) minComfortableScale else scale
                } else {
                    scale
                }

                // 3. Horizontal centering of the column/word when zoomed
                val targetOffsetX = if (req.matchIndex >= 0 && targetScale > 1.05f && targetRect != null && pageWidth > 0) {
                    val maxOffsetX = (viewportWidthPx * (targetScale - 1f)) / 2f
                    val matchCenterX = (targetRect.left + targetRect.right) / (2f * pageWidth)
                    val deltaX = (0.5f - matchCenterX) * contentWidthPx * targetScale
                    deltaX.coerceIn(-maxOffsetX, maxOffsetX)
                } else {
                    if (targetScale <= 1.05f) 0f else offset.x
                }

                // Animate scale & horizontal offset concurrently with vertical scroll
                if (req.matchIndex >= 0) {
                    launch {
                        if (kotlin.math.abs(scale - targetScale) > 0.01f) {
                            animate(scale, targetScale, animationSpec = tween(320, easing = FastOutSlowInEasing)) { value, _ ->
                                scale = value
                            }
                        }
                    }
                    launch {
                        if (kotlin.math.abs(offset.x - targetOffsetX) > 1f) {
                            animate(offset.x, targetOffsetX, animationSpec = tween(320, easing = FastOutSlowInEasing)) { value, _ ->
                                offset = Offset(value, offset.y)
                            }
                        }
                    }
                }

                performPageJumpWithOffset(safeIndex, scrollOffsetPx, req.isSmoothScroll, true)
                onRequestPage(safeIndex)
            }
        }

        // Document Content
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 28.dp, // Reserve comfortable clearance for the right-side scrubber
                top = 8.dp,  // Minimal top padding to maximize reading area under hidden status bar
                bottom = 72.dp // Reserve clearance for floating progress pill
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(
                count = state.totalPages,
                key = { pageIndex -> pageIndex }
            ) { pageIndex ->
                val pageBitmap = state.pageBitmaps[pageIndex]
                val dimensions = state.pageDimensions[pageIndex]
                val aspectRatio = dimensions?.aspectRatio ?: 0.707f

                // Request rasterization when item is composed
                LaunchedEffect(pageIndex) {
                    onRequestPage(pageIndex)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    val currentSelectionState by rememberUpdatedState(selection)
                    val currentOnClearSelection by rememberUpdatedState(onClearSelection)
                    val currentOnToggleControls by rememberUpdatedState(onToggleControls)
                    val currentOnSelectWordAt by rememberUpdatedState(onSelectWordAt)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pageIndex) {
                                detectTapGestures(
                                    onTap = {
                                        if (currentSelectionState != null) {
                                            currentOnClearSelection()
                                        } else {
                                            currentOnToggleControls()
                                        }
                                    },
                                    onLongPress = { offsetPx ->
                                        val pageWidth = (dimensions?.width?.toFloat() ?: size.width.toFloat()).coerceAtLeast(1f)
                                        val pageHeight = (dimensions?.height?.toFloat() ?: size.height.toFloat()).coerceAtLeast(1f)
                                        val scaleX = size.width / pageWidth
                                        val scaleY = size.height / pageHeight
                                        val pdfX = offsetPx.x / scaleX
                                        val pdfY = offsetPx.y / scaleY
                                        currentOnSelectWordAt(pageIndex, pdfX, pdfY)
                                    }
                                )
                            }
                    ) {
                        if (pageBitmap != null) {
                            Image(
                                bitmap = pageBitmap.asImageBitmap(),
                                contentDescription = stringResource(
                                    R.string.page_indicator,
                                    pageIndex + 1,
                                    state.totalPages
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = ReaderAccentGrey,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Page ${pageIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        // Search Highlight Overlay layer for this page
                        if (searchState.isSearchActive && searchState.matches.isNotEmpty()) {
                            val pageMatches = searchState.matches.filter { it.pageIndex == pageIndex }
                            if (pageMatches.isNotEmpty()) {
                                val activeMatch = searchState.matches.getOrNull(searchState.currentMatchIndex)
                                SearchHighlightOverlay(
                                    pageMatches = pageMatches,
                                    activeMatch = if (activeMatch?.pageIndex == pageIndex) activeMatch else null,
                                    pageDimensions = dimensions,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Dedicated Native-Looking Blue Text Selection Overlay
                        if (selection != null && selection.pageIndex == pageIndex) {
                            PdfTextSelectionOverlay(
                                selection = selection,
                                pageDimensions = dimensions,
                                onUpdateBoundary = { pdfX, pdfY, isStart ->
                                    onExtendSelectionTo(pageIndex, pdfX, pdfY, isStart)
                                },
                                onStartDrag = onStartSelectionDrag,
                                onEndDrag = onEndSelectionDrag,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Draggable Fast-Scroll Scrubber along the right edge
        FastScrollScrubber(
            currentPageIndex = currentVisibleItem,
            totalPages = state.totalPages,
            onScrubToPage = { targetIndex, isCommit ->
                performPageJump(targetIndex, false, isCommit)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(vertical = 56.dp)
        )

        // Floating Text Selection Action Toolbar (Copy, Select All, Close)
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current

        AnimatedVisibility(
            visible = selection != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            if (selection != null) {
                TextSelectionToolbar(
                    selection = selection,
                    onCopy = { textToCopy ->
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onSelectAll = {
                        onSelectAllOnPage(selection.pageIndex)
                    },
                    onClose = onClearSelection
                )
            }
        }

        // Search Interface (when search is active)
        AnimatedVisibility(
            visible = searchState.isSearchActive,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
        ) {
            ReaderSearchOverlay(
                searchState = searchState,
                onQueryChanged = onSearchQueryChanged,
                onNextMatch = onNextSearchMatch,
                onPreviousMatch = onPreviousSearchMatch,
                onCloseSearch = onCloseSearch
            )
        }

        // Bottom Controls Overlay or Minimal Immersive Pill
        if (!searchState.isSearchActive) {
            if (isControlsVisible) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ReaderBottomControlsOverlay(
                        state = state,
                        currentVisibleIndex = currentVisibleItem,
                        scale = scale,
                        onPreviousPage = {
                            val prev = (currentVisibleItem - 1).coerceAtLeast(0)
                            performPageJump(prev, true, true)
                            onPreviousPage()
                        },
                        onNextPage = {
                            val next = (currentVisibleItem + 1).coerceAtMost(state.totalPages - 1)
                            performPageJump(next, true, true)
                            onNextPage()
                        },
                        onOpenJumpDialog = onOpenJumpDialog,
                        onOpenSearch = onOpenSearch,
                        onClose = onClose,
                        onSelectAnotherFile = onSelectAnotherFile,
                        onZoomIn = {
                            scale = (scale + 0.5f).coerceIn(1f, 4f)
                        },
                        onZoomOut = {
                            val newScale = (scale - 0.5f).coerceIn(1f, 4f)
                            scale = newScale
                            if (newScale <= 1f) {
                                offset = Offset.Zero
                            }
                        },
                        onResetZoom = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        onHideControls = onToggleControls
                    )
                }
            } else {
                // Immersive Reading State: Sleek, unobtrusive progress pill
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ImmersiveReadingPill(
                        currentPageOneBased = currentVisibleItem + 1,
                        totalPages = state.totalPages,
                        scale = scale,
                        onOpenJumpDialog = onOpenJumpDialog,
                        onOpenSearch = onOpenSearch,
                        onToggleControls = onToggleControls,
                        onResetZoom = {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                }
            }
        }
    }
}

/**
 * Draggable vertical fast-scroll scrubber placed along the right edge of the reader.
 * Reflects actual scroll position, supports high-speed dragging, and shows a single-line floating tooltip.
 */
@Composable
private fun FastScrollScrubber(
    currentPageIndex: Int,
    totalPages: Int,
    onScrubToPage: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var scrubPageIndex by remember { mutableIntStateOf(currentPageIndex) }

    val thumbHeightDp = 48.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(44.dp) // Wide touch target for comfortable finger dragging
            .semantics {
                contentDescription = "Fast scroll bar. Drag vertically to navigate pages quickly."
            }
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val availableTravelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

        // When dragging, thumb follows touch; when idle, thumb reflects current document page
        val activePageIndex = if (isDragging) scrubPageIndex else currentPageIndex
        val fraction = ReadingProgress.calculateScrubberFraction(activePageIndex, totalPages)
        val thumbOffsetY = fraction * availableTravelPx

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(totalPages) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val clampedY = (offset.y - thumbHeightPx / 2f).coerceIn(0f, availableTravelPx)
                            val scrubFraction = clampedY / availableTravelPx
                            val targetIndex = ReadingProgress.pageIndexFromScrubberFraction(scrubFraction, totalPages)
                            scrubPageIndex = targetIndex
                            onScrubToPage(targetIndex, false)
                        },
                        onDragEnd = {
                            isDragging = false
                            onScrubToPage(scrubPageIndex, true)
                        },
                        onDragCancel = {
                            isDragging = false
                            onScrubToPage(scrubPageIndex, true)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val clampedY = (change.position.y - thumbHeightPx / 2f).coerceIn(0f, availableTravelPx)
                            val scrubFraction = clampedY / availableTravelPx
                            val targetIndex = ReadingProgress.pageIndexFromScrubberFraction(scrubFraction, totalPages)
                            if (targetIndex != scrubPageIndex) {
                                scrubPageIndex = targetIndex
                                onScrubToPage(targetIndex, false)
                            }
                        }
                    )
                }
        ) {
            // Track background line
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(ReaderScrubberTrack)
            )

            // Live floating page tooltip bubble shown while scrubbing (strictly single-line numeric indicator)
            if (isDragging) {
                val previewPageOneBased = scrubPageIndex + 1

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentWidth(unbounded = true)
                        .offset {
                            IntOffset(
                                x = -with(density) { 54.dp.roundToPx() },
                                y = thumbOffsetY.roundToInt()
                            )
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = ReaderGreyBg,
                    border = BorderStroke(1.dp, ReaderGreyBorder),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = previewPageOneBased.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ReaderTextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Draggable Thumb
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp)
                    .offset { IntOffset(0, thumbOffsetY.roundToInt()) }
                    .width(if (isDragging) 8.dp else 6.dp)
                    .height(thumbHeightDp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isDragging) ReaderScrubberThumbActive else ReaderScrubberThumb
                    )
                    .shadow(elevation = if (isDragging) 4.dp else 1.dp, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

/**
 * Compact inline text search overlay associated with the reader bottom toolbar.
 */
@Composable
private fun ReaderSearchOverlay(
    searchState: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = ReaderGreyBg,
        border = BorderStroke(1.dp, ReaderGreyBorder),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Dismiss search
            IconButton(
                onClick = onCloseSearch,
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = "Close search" }
            ) {
                Icon(
                    imageVector = ReaderIcons.Close,
                    contentDescription = null,
                    tint = ReaderTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Compact text input field
            BasicTextField(
                value = searchState.query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = ReaderTextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(ReaderTextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNextMatch() }),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchState.query.isEmpty()) {
                            Text(
                                text = "Search in PDF…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ReaderTextSecondary
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Clear query button
            if (searchState.query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChanged("") },
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = "Clear query" }
                ) {
                    Icon(
                        imageVector = ReaderIcons.SearchClear,
                        contentDescription = null,
                        tint = ReaderTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Match counter display and loading indicator
            if (searchState.isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = ReaderTextSecondary
                )
            }

            val matchDisplay = searchState.currentMatchDisplay
            if (matchDisplay.isNotBlank()) {
                Text(
                    text = matchDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (searchState.totalMatches > 0) ReaderTextPrimary else ReaderTextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Previous match button
            IconButton(
                onClick = onPreviousMatch,
                enabled = searchState.matches.isNotEmpty(),
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = "Previous match" }
            ) {
                Icon(
                    imageVector = ReaderIcons.SearchUp,
                    contentDescription = null,
                    tint = if (searchState.matches.isNotEmpty()) ReaderTextPrimary else ReaderTextSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Next match button
            IconButton(
                onClick = onNextMatch,
                enabled = searchState.matches.isNotEmpty(),
                modifier = Modifier
                    .size(36.dp)
                    .semantics { contentDescription = "Next match" }
            ) {
                Icon(
                    imageVector = ReaderIcons.SearchDown,
                    contentDescription = null,
                    tint = if (searchState.matches.isNotEmpty()) ReaderTextPrimary else ReaderTextSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Bottom controls overlay containing compact icon buttons for navigation, search, file operations,
 * zoom adjustments, and immersive reading dismiss toggle.
 */
@Composable
private fun ReaderBottomControlsOverlay(
    state: PdfViewerUiState.Ready,
    currentVisibleIndex: Int,
    scale: Float,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenJumpDialog: () -> Unit,
    onOpenSearch: () -> Unit,
    onClose: () -> Unit,
    onSelectAnotherFile: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onHideControls: () -> Unit
) {
    val currentPageOneBased = currentVisibleIndex + 1
    val progressText = ReadingProgress.formatProgress(currentPageOneBased, state.totalPages)
    val percentage = ReadingProgress.calculatePercentage(currentPageOneBased, state.totalPages)

    val zoomInDesc = stringResource(R.string.action_zoom_in)
    val zoomOutDesc = stringResource(R.string.action_zoom_out)
    val resetZoomDesc = stringResource(R.string.action_reset_zoom)
    val hideControlsDesc = stringResource(R.string.action_hide_controls)
    val openAnotherPdfDesc = stringResource(R.string.action_open_another_pdf)

    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        color = ReaderGreyBg,
        border = BorderStroke(1.dp, ReaderGreyBorder),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Close / Exit Reader Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(38.dp)
                    .semantics { contentDescription = "Exit reader" }
            ) {
                Icon(
                    imageVector = ReaderIcons.Close,
                    contentDescription = null,
                    tint = ReaderTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Open Another PDF
            IconButton(
                onClick = onSelectAnotherFile,
                modifier = Modifier
                    .size(38.dp)
                    .semantics { contentDescription = openAnotherPdfDesc }
            ) {
                Icon(
                    imageVector = ReaderIcons.OpenFile,
                    contentDescription = null,
                    tint = ReaderTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Search Button
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .size(38.dp)
                    .semantics { contentDescription = "Search text" }
            ) {
                Icon(
                    imageVector = ReaderIcons.Search,
                    contentDescription = null,
                    tint = ReaderTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Previous Page Button
            IconButton(
                onClick = onPreviousPage,
                enabled = currentVisibleIndex > 0,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = ReaderTextPrimary,
                    disabledContentColor = ReaderTextSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .size(38.dp)
                    .semantics { contentDescription = "Previous page" }
            ) {
                Icon(
                    imageVector = ReaderIcons.PreviousPage,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Compact Reading Progress Pill (Tappable for Page Jump)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ReaderGreyElevated,
                border = BorderStroke(1.dp, ReaderGreyBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Jump to page",
                        onClick = onOpenJumpDialog
                    )
                    .semantics {
                        contentDescription = "Page $currentPageOneBased of ${state.totalPages}, $percentage% complete. Tap to jump."
                    }
            ) {
                Text(
                    text = progressText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = ReaderTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Next Page Button
            IconButton(
                onClick = onNextPage,
                enabled = currentVisibleIndex < state.totalPages - 1,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = ReaderTextPrimary,
                    disabledContentColor = ReaderTextSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .size(38.dp)
                    .semantics { contentDescription = "Next page" }
            ) {
                Icon(
                    imageVector = ReaderIcons.NextPage,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Zoom In Button
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(38.dp)
                    .semantics { contentDescription = zoomInDesc }
            ) {
                Icon(
                    imageVector = ReaderIcons.ZoomIn,
                    contentDescription = null,
                    tint = ReaderTextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Zoom Out Button
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(38.dp)
                    .semantics { contentDescription = zoomOutDesc }
            ) {
                Icon(
                    imageVector = ReaderIcons.ZoomOut,
                    contentDescription = null,
                    tint = ReaderTextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Reset Zoom Button (visible when zoomed in)
            if (scale > 1.05f) {
                IconButton(
                    onClick = onResetZoom,
                    modifier = Modifier
                        .size(38.dp)
                        .semantics { contentDescription = resetZoomDesc }
                ) {
                    Icon(
                        imageVector = ReaderIcons.ResetZoom,
                        contentDescription = null,
                        tint = ReaderTextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Hide Controls
            IconButton(
                onClick = onHideControls,
                modifier = Modifier
                    .size(38.dp)
                    .semantics { contentDescription = hideControlsDesc }
            ) {
                Icon(
                    imageVector = ReaderIcons.HideControls,
                    contentDescription = null,
                    tint = ReaderTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Minimalist floating pill shown during immersive reading state.
 * Consumes minimal screen space and provides progress display, search trigger, and controls toggle.
 */
@Composable
private fun ImmersiveReadingPill(
    currentPageOneBased: Int,
    totalPages: Int,
    scale: Float,
    onOpenJumpDialog: () -> Unit,
    onOpenSearch: () -> Unit,
    onToggleControls: () -> Unit,
    onResetZoom: () -> Unit
) {
    val progressText = ReadingProgress.formatProgress(currentPageOneBased, totalPages)
    val percentage = ReadingProgress.calculatePercentage(currentPageOneBased, totalPages)
    val resetZoomDesc = stringResource(R.string.action_reset_zoom)
    val toggleControlsDesc = stringResource(R.string.action_toggle_controls)

    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = ReaderGreyBg,
        border = BorderStroke(1.dp, ReaderGreyBorder),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Tappable Reading Progress Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Jump to page",
                        onClick = onOpenJumpDialog
                    )
                    .semantics {
                        contentDescription = "Page $currentPageOneBased of $totalPages, $percentage% complete. Tap to jump to page."
                    }
            ) {
                Text(
                    text = progressText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = ReaderTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Direct Search Button in pill
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .size(32.dp)
                    .semantics { contentDescription = "Search text" }
            ) {
                Icon(
                    imageVector = ReaderIcons.Search,
                    contentDescription = null,
                    tint = ReaderTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // If zoomed in, show a reset zoom button
            if (scale > 1.05f) {
                IconButton(
                    onClick = onResetZoom,
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = resetZoomDesc }
                ) {
                    Icon(
                        imageVector = ReaderIcons.ResetZoom,
                        contentDescription = null,
                        tint = ReaderTextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Compact button to reveal reader controls
            IconButton(
                onClick = onToggleControls,
                modifier = Modifier
                    .size(32.dp)
                    .semantics { contentDescription = toggleControlsDesc }
            ) {
                Icon(
                    imageVector = ReaderIcons.ShowControls,
                    contentDescription = null,
                    tint = ReaderTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Accessible direct page navigation dialog combining numeric text entry and a slider.
 */
@Composable
private fun JumpToPageDialog(
    currentPageOneBased: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onConfirmJump: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf(currentPageOneBased.toString()) }
    var sliderValue by remember { mutableFloatStateOf(currentPageOneBased.toFloat()) }
    var validationResult by remember {
        mutableStateOf<PageValidationResult>(
            PageValidationResult.Valid(currentPageOneBased, currentPageOneBased - 1)
        )
    }

    LaunchedEffect(inputText) {
        validationResult = PageValidationResult.validate(inputText, totalPages)
        if (validationResult is PageValidationResult.Valid) {
            sliderValue = (validationResult as PageValidationResult.Valid).pageNumberOneBased.toFloat()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.jump_to_page_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.jump_to_page_label, totalPages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        inputText = filtered
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = validationResult is PageValidationResult.Invalid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (validationResult is PageValidationResult.Valid) {
                                onConfirmJump((validationResult as PageValidationResult.Valid).pageNumberOneBased)
                            }
                        }
                    ),
                    supportingText = {
                        if (validationResult is PageValidationResult.Invalid) {
                            Text(
                                text = (validationResult as PageValidationResult.Invalid).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                if (totalPages > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = { newVal ->
                            val rounded = newVal.toInt().coerceIn(1, totalPages)
                            sliderValue = rounded.toFloat()
                            inputText = rounded.toString()
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = if (totalPages > 2) totalPages - 2 else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = ReaderTextPrimary,
                            activeTrackColor = ReaderAccentGrey,
                            inactiveTrackColor = ReaderGreyBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick jump shortcut chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        inputText = "1"
                        sliderValue = 1f
                    }) {
                        Text(text = stringResource(R.string.action_first_page))
                    }

                    TextButton(onClick = {
                        inputText = totalPages.toString()
                        sliderValue = totalPages.toFloat()
                    }) {
                        Text(text = stringResource(R.string.action_last_page, totalPages))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validationResult is PageValidationResult.Valid) {
                        onConfirmJump((validationResult as PageValidationResult.Valid).pageNumberOneBased)
                    }
                },
                enabled = validationResult is PageValidationResult.Valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReaderAccentGrey,
                    contentColor = ReaderTextPrimary
                )
            ) {
                Text(text = stringResource(R.string.action_jump))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun LoadingViewerState(
    message: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(44.dp)
                .semantics { contentDescription = "Close PDF viewer" }
        ) {
            Icon(
                imageVector = ReaderIcons.Close,
                contentDescription = null,
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(52.dp),
                color = ReaderTextPrimary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyViewerState(
    onSelectFile: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(44.dp)
                .semantics { contentDescription = "Close PDF viewer" }
        ) {
            Icon(
                imageVector = ReaderIcons.Close,
                contentDescription = null,
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ReaderGreyElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📄",
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Document Selected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose a PDF file from your device to begin viewing.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSelectFile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReaderAccentGrey,
                    contentColor = ReaderTextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_open_pdf),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ErrorViewerState(
    state: PdfViewerUiState.Error,
    onSelectAnotherFile: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(44.dp)
                .semantics { contentDescription = "Close PDF viewer" }
        ) {
            Icon(
                imageVector = ReaderIcons.Close,
                contentDescription = null,
                tint = Color.White
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 36.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSelectAnotherFile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReaderAccentGrey,
                        contentColor = ReaderTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_select_another_file),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Visual highlight overlay rendered directly above a PDF page bitmap for search results.
 * Transforms typographic point coordinates to Canvas pixels to achieve pixel-perfect alignment
 * with the underlying text across any zoom, scroll, or pan state.
 *
 * Preserves the original PDF document rendering and typography without redrawing text using
 * generic Android system fonts.
 * - Inactive matches: subtle translucent blue highlight.
 * - Active match: distinct, vibrant blue selection highlight.
 */
@Composable
private fun SearchHighlightOverlay(
    pageMatches: List<com.freedoc.app.engine.pdf.SearchMatch>,
    activeMatch: com.freedoc.app.engine.pdf.SearchMatch?,
    pageDimensions: PageDimensions?,
    modifier: Modifier = Modifier
) {
    // Inactive search matches: subtle translucent blue (~20% alpha)
    val otherMatchColor = Color(0x333B82F6)
    // Active search match: distinct, vibrant selection blue (~42% alpha) preserving original PDF text
    val activeMatchColor = Color(0x6B1A73E8)

    Canvas(modifier = modifier) {
        val pageWidth = (pageDimensions?.width?.toFloat() ?: size.width).coerceAtLeast(1f)
        val pageHeight = (pageDimensions?.height?.toFloat() ?: size.height).coerceAtLeast(1f)
        val scaleX = size.width / pageWidth
        val scaleY = size.height / pageHeight
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        // 1. Draw secondary indication for inactive matches on this page
        for (match in pageMatches) {
            if (match == activeMatch) continue
            for (rect in match.highlightRects) {
                val left = rect.left * scaleX
                val top = rect.top * scaleY
                val width = rect.width * scaleX
                val height = rect.height * scaleY
                if (width > 0 && height > 0) {
                    drawRoundRect(
                        color = otherMatchColor,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }

        // 2. Draw distinct blue selection background for active search match preserving original PDF text
        if (activeMatch != null) {
            for (rect in activeMatch.highlightRects) {
                val left = rect.left * scaleX
                val top = rect.top * scaleY
                val width = rect.width * scaleX
                val height = rect.height * scaleY
                if (width > 0 && height > 0) {
                    drawRoundRect(
                        color = activeMatchColor,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Native-Looking Blue Text Selection Overlay for active user text selection.
 *
 * Implements acceptance criteria:
 * - High-contrast, clean translucent royal blue selection background (Color(0x59005FB8))
 * - Preserves original PDF typography, embedded fonts, weights, ligatures, and sharpness
 * - Accurate line-grouped boundaries that follow actual glyph bounds without giant paragraph blocks
 * - Smooth, latency-free boundary dragging with circular pins and teardrop cursor lines
 * - Full multi-line expansion/contraction in both directions
 * - Zero font recreation with generic Android fonts or artificial bolding
 */
@Composable
private fun PdfTextSelectionOverlay(
    selection: PdfSelection,
    pageDimensions: PageDimensions?,
    onUpdateBoundary: (pdfX: Float, pdfY: Float, isStart: Boolean) -> Unit,
    onStartDrag: (isStart: Boolean) -> Unit = {},
    onEndDrag: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Rich royal blue matching native document reader (translucent overlay + solid handles)
    val selectionHighlightColor = Color(0x59005FB8)
    val handleColor = Color(0xFF005FB8)

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val pageWidth = (pageDimensions?.width?.toFloat() ?: widthPx).coerceAtLeast(1f)
        val pageHeight = (pageDimensions?.height?.toFloat() ?: heightPx).coerceAtLeast(1f)
        val scaleX = widthPx / pageWidth
        val scaleY = heightPx / pageHeight

        val startBound = selection.startHandleRect ?: selection.lineRects.firstOrNull()
        val startX = (startBound?.left ?: 0f) * scaleX
        val startTop = (startBound?.top ?: 0f) * scaleY
        val startBottom = (startBound?.bottom ?: 0f) * scaleY

        val endBound = selection.endHandleRect ?: selection.lineRects.lastOrNull()
        val endX = (endBound?.right ?: 0f) * scaleX
        val endTop = (endBound?.top ?: 0f) * scaleY
        val endBottom = (endBound?.bottom ?: 0f) * scaleY

        val currentOnUpdateBoundary by rememberUpdatedState(onUpdateBoundary)
        val currentOnStartDrag by rememberUpdatedState(onStartDrag)
        val currentOnEndDrag by rememberUpdatedState(onEndDrag)
        val currentScaleX by rememberUpdatedState(scaleX)
        val currentScaleY by rememberUpdatedState(scaleY)
        val currentStartX by rememberUpdatedState(startX)
        val currentStartBottom by rememberUpdatedState(startBottom)
        val currentEndX by rememberUpdatedState(endX)
        val currentEndBottom by rememberUpdatedState(endBottom)

        // Overlay with robust handle touch arbitration
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position

                        val startPinCenter = Offset(currentStartX, currentStartBottom + 7.dp.toPx())
                        val endPinCenter = Offset(currentEndX, currentEndBottom + 7.dp.toPx())
                        val touchRadius = 36.dp.toPx()

                        val distToStart = (downPos - startPinCenter).getDistance()
                        val distToEnd = (downPos - endPinCenter).getDistance()

                        val isStartGrabbed = distToStart <= touchRadius && (distToStart <= distToEnd)
                        val isEndGrabbed = !isStartGrabbed && distToEnd <= touchRadius

                        if (isStartGrabbed || isEndGrabbed) {
                            val isStart = isStartGrabbed
                            down.consume()
                            currentOnStartDrag(isStart)

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break

                                    change.consume()
                                    val pdfX = change.position.x / currentScaleX
                                    val pdfY = (change.position.y - 10.dp.toPx()) / currentScaleY
                                    currentOnUpdateBoundary(pdfX, pdfY, isStart)
                                }
                            } finally {
                                currentOnEndDrag()
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

                // 1. Draw translucent blue selection background for each line, preserving original PDF text
                for (rect in selection.lineRects) {
                    val left = rect.left * scaleX
                    val top = rect.top * scaleY
                    val width = rect.width * scaleX
                    val height = rect.height * scaleY
                    if (width > 0 && height > 0) {
                        drawRoundRect(
                            color = selectionHighlightColor,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            cornerRadius = cornerRadius
                        )
                    }
                }

                // 2. Draw start handle (vertical cursor line and teardrop pin)
                if (startBound != null) {
                    val pinRadius = 7.dp.toPx()
                    val pinCenter = Offset(startX, startBottom + pinRadius)

                    // Vertical cursor line
                    drawLine(
                        color = handleColor,
                        start = Offset(startX, startTop),
                        end = Offset(startX, startBottom),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Solid blue pin
                    drawCircle(
                        color = handleColor,
                        radius = pinRadius,
                        center = pinCenter
                    )
                    // High-contrast inner white dot
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = pinCenter
                    )
                }

                // 3. Draw end handle (vertical cursor line and teardrop pin)
                if (endBound != null) {
                    val pinRadius = 7.dp.toPx()
                    val pinCenter = Offset(endX, endBottom + pinRadius)

                    // Vertical cursor line
                    drawLine(
                        color = handleColor,
                        start = Offset(endX, endTop),
                        end = Offset(endX, endBottom),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Solid blue pin
                    drawCircle(
                        color = handleColor,
                        radius = pinRadius,
                        center = pinCenter
                    )
                    // High-contrast inner white dot
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = pinCenter
                    )
                }
            }
        }
    }
}

/**
 * Floating context toolbar displayed above active text selection offering Copy, Select All, and Clear.
 */
@Composable
private fun TextSelectionToolbar(
    selection: PdfSelection,
    onCopy: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = ReaderGreyElevated,
        border = BorderStroke(1.dp, ReaderGreyBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(
                onClick = { onCopy(selection.text) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = ReaderIcons.ContentCopy,
                    contentDescription = "Copy selected text",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Copy",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(ReaderGreyBorder)
            )

            TextButton(
                onClick = onSelectAll,
                colors = ButtonDefaults.textButtonColors(contentColor = ReaderTextSecondary)
            ) {
                Text(
                    text = "Select all",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = ReaderIcons.Close,
                    contentDescription = "Clear selection",
                    modifier = Modifier.size(16.dp),
                    tint = ReaderTextSecondary
                )
            }
        }
    }
}
