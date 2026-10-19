package com.freedoc.app.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedoc.app.R
import com.freedoc.app.ui.theme.NavyPrimary
import com.freedoc.app.ui.theme.TealAccent

/**
 * Compose screen for rendering and interacting with PDF documents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uiState: PdfViewerUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSelectAnotherFile: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ViewerTopBar(
                uiState = uiState,
                onClose = onClose,
                onSelectAnotherFile = onSelectAnotherFile
            )
        },
        bottomBar = {
            if (uiState is PdfViewerUiState.Ready) {
                ViewerBottomBar(
                    state = uiState,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is PdfViewerUiState.Idle -> {
                    EmptyViewerState(onSelectFile = onSelectAnotherFile)
                }
                is PdfViewerUiState.Loading -> {
                    LoadingViewerState(message = uiState.message)
                }
                is PdfViewerUiState.Ready -> {
                    PdfPageView(
                        state = uiState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is PdfViewerUiState.Error -> {
                    ErrorViewerState(
                        state = uiState,
                        onSelectAnotherFile = onSelectAnotherFile
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerTopBar(
    uiState: PdfViewerUiState,
    onClose: () -> Unit,
    onSelectAnotherFile: () -> Unit
) {
    val title = when (uiState) {
        is PdfViewerUiState.Ready -> uiState.metadata.displayName
        is PdfViewerUiState.Loading -> stringResource(R.string.loading_document)
        else -> stringResource(R.string.app_name)
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (uiState is PdfViewerUiState.Ready && uiState.metadata.sizeBytes > 0) {
                    Text(
                        text = uiState.metadata.formattedSize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Text(
                    text = "✕",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            FilledTonalButton(
                onClick = onSelectAnotherFile,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = TealAccent.copy(alpha = 0.15f),
                    contentColor = NavyPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_open_another_pdf),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}

@Composable
private fun PdfPageView(
    state: PdfViewerUiState.Ready,
    modifier: Modifier = Modifier
) {
    // Reset zoom and pan upon page navigation
    var scale by remember(state.currentPage) { mutableFloatStateOf(1f) }
    var offset by remember(state.currentPage) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF202225)) // Neutral dark backdrop for reading contrast
            .pointerInput(state.currentPage) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = newScale
                    if (newScale > 1f) {
                        // Allow panning proportionally to scale factor
                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x * newScale).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Document Page Bitmap
        if (state.pageBitmap != null) {
            Card(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Image(
                    bitmap = state.pageBitmap.asImageBitmap(),
                    contentDescription = stringResource(
                        R.string.page_indicator,
                        state.displayPageNumber,
                        state.totalPages
                    )
                )
            }
        }

        // Overlay: Reset Zoom Button when zoomed in
        AnimatedVisibility(
            visible = scale > 1.05f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                shape = RoundedCornerShape(20.dp),
                color = NavyPrimary.copy(alpha = 0.9f),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = stringResource(R.string.action_reset_zoom),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Subtle loading spinner overlay while switching pages
        if (state.isPageLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = TealAccent,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun ViewerBottomBar(
    state: PdfViewerUiState.Ready,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Page Button
            OutlinedButton(
                onClick = onPreviousPage,
                enabled = state.hasPreviousPage,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "◀ " + stringResource(R.string.action_previous_page),
                    fontWeight = FontWeight.Medium
                )
            }

            // Current Page Indicator Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TealAccent.copy(alpha = 0.15f)
            ) {
                Text(
                    text = stringResource(
                        R.string.page_indicator,
                        state.displayPageNumber,
                        state.totalPages
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Next Page Button
            Button(
                onClick = onNextPage,
                enabled = state.hasNextPage,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimary,
                    disabledContainerColor = NavyPrimary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_next_page) + " ▶",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LoadingViewerState(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = TealAccent,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyViewerState(onSelectFile: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TealAccent.copy(alpha = 0.15f)),
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
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose a PDF file from your device to begin viewing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSelectFile,
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.action_open_pdf),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ErrorViewerState(
    state: PdfViewerUiState.Error,
    onSelectAnotherFile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
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
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
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
