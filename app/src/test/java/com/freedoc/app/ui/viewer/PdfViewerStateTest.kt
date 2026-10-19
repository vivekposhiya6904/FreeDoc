package com.freedoc.app.ui.viewer

import com.freedoc.app.core.model.DocumentMetadata
import com.freedoc.app.engine.pdf.PdfErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PdfViewerStateTest {

    private val sampleMetadata = DocumentMetadata(
        uriString = "content://dummy/document.pdf",
        displayName = "Sample.pdf",
        sizeBytes = 1024,
        pageCount = 5
    )

    @Test
    fun testReadyStateNavigationBoundariesForMultiPageDocument() {
        // Multi-page document: 5 pages (indexes 0..4)

        // First page: index 0
        val firstPageState = createReadyState(currentPage = 0, totalPages = 5)
        assertFalse("First page should not have previous page", firstPageState.hasPreviousPage)
        assertTrue("First page should have next page", firstPageState.hasNextPage)
        assertEquals(1, firstPageState.displayPageNumber)

        // Middle page: index 2
        val middlePageState = createReadyState(currentPage = 2, totalPages = 5)
        assertTrue("Middle page should have previous page", middlePageState.hasPreviousPage)
        assertTrue("Middle page should have next page", middlePageState.hasNextPage)
        assertEquals(3, middlePageState.displayPageNumber)

        // Last page: index 4
        val lastPageState = createReadyState(currentPage = 4, totalPages = 5)
        assertTrue("Last page should have previous page", lastPageState.hasPreviousPage)
        assertFalse("Last page should not have next page", lastPageState.hasNextPage)
        assertEquals(5, lastPageState.displayPageNumber)
    }

    @Test
    fun testReadyStateNavigationBoundariesForSinglePageDocument() {
        // Single-page document: 1 page (index 0)
        val singlePageState = createReadyState(currentPage = 0, totalPages = 1)
        assertFalse("Single-page document cannot have previous page", singlePageState.hasPreviousPage)
        assertFalse("Single-page document cannot have next page", singlePageState.hasNextPage)
        assertEquals(1, singlePageState.displayPageNumber)
    }

    @Test
    fun testErrorStateProperties() {
        val errorState = PdfViewerUiState.Error(
            errorType = PdfErrorType.PASSWORD_PROTECTED,
            title = "Password-Protected PDF",
            message = "This PDF requires a password to open."
        )

        assertEquals(PdfErrorType.PASSWORD_PROTECTED, errorState.errorType)
        assertEquals("Password-Protected PDF", errorState.title)
        assertEquals("This PDF requires a password to open.", errorState.message)
    }

    @Test
    fun testLoadingStateDefaultMessage() {
        val loadingState = PdfViewerUiState.Loading()
        assertEquals("Opening document…", loadingState.message)

        val customLoading = PdfViewerUiState.Loading("Rendering page 3…")
        assertEquals("Rendering page 3…", customLoading.message)
    }

    private fun createReadyState(currentPage: Int, totalPages: Int): PdfViewerUiState.Ready {
        return PdfViewerUiState.Ready(
            metadata = sampleMetadata.copy(pageCount = totalPages),
            currentPage = currentPage,
            totalPages = totalPages,
            pageBitmap = null
        )
    }
}
