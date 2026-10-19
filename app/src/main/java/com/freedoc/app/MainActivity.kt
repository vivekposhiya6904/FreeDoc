package com.freedoc.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.freedoc.app.ui.HomeScreen
import com.freedoc.app.ui.theme.FreeDocTheme
import com.freedoc.app.ui.viewer.PdfViewerScreen
import com.freedoc.app.ui.viewer.PdfViewerUiState
import com.freedoc.app.ui.viewer.PdfViewerViewModel

enum class AppScreen {
    HOME,
    VIEWER
}

class MainActivity : ComponentActivity() {

    private val viewModel: PdfViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialUri: Uri? = if (intent?.action == Intent.ACTION_VIEW) intent.data else null

        setContent {
            FreeDocTheme {
                FreeDocApp(
                    viewModel = viewModel,
                    initialUri = initialUri
                )
            }
        }
    }
}

@Composable
fun FreeDocApp(
    viewModel: PdfViewerViewModel,
    initialUri: Uri? = null
) {
    val context = LocalContext.current
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    val uiState by viewModel.uiState.collectAsState()

    // Storage Access Framework Document Picker for PDF files
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.openDocument(context, uri)
            currentScreen = AppScreen.VIEWER
        }
        // If uri is null, the user cancelled the picker; remain on current screen gracefully
    }

    // Handle initial intent URI if opened externally
    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            viewModel.openDocument(context, initialUri)
            currentScreen = AppScreen.VIEWER
        }
    }

    // Intercept hardware/system back button while viewing a document
    BackHandler(enabled = currentScreen == AppScreen.VIEWER) {
        currentScreen = AppScreen.HOME
    }

    val activeDocumentName = when (val state = uiState) {
        is PdfViewerUiState.Ready -> state.metadata.displayName
        else -> null
    }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                onOpenPdfClicked = {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                },
                activeDocumentName = activeDocumentName,
                onResumeViewerClicked = {
                    currentScreen = AppScreen.VIEWER
                }
            )
        }
        AppScreen.VIEWER -> {
            PdfViewerScreen(
                uiState = uiState,
                onPreviousPage = { viewModel.previousPage() },
                onNextPage = { viewModel.nextPage() },
                onSelectAnotherFile = {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                },
                onClose = {
                    currentScreen = AppScreen.HOME
                }
            )
        }
    }
}
