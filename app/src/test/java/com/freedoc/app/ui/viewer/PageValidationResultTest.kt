package com.freedoc.app.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PageValidationResultTest {

    @Test
    fun testValidPageNumbersWithinRange() {
        val totalPages = 20

        // First page
        val firstResult = PageValidationResult.validate("1", totalPages)
        assertTrue(firstResult is PageValidationResult.Valid)
        val firstValid = firstResult as PageValidationResult.Valid
        assertEquals(1, firstValid.pageNumberOneBased)
        assertEquals(0, firstValid.targetIndexZeroBased)

        // Intermediate page
        val middleResult = PageValidationResult.validate("10", totalPages)
        assertTrue(middleResult is PageValidationResult.Valid)
        val middleValid = middleResult as PageValidationResult.Valid
        assertEquals(10, middleValid.pageNumberOneBased)
        assertEquals(9, middleValid.targetIndexZeroBased)

        // Last page
        val lastResult = PageValidationResult.validate("20", totalPages)
        assertTrue(lastResult is PageValidationResult.Valid)
        val lastValid = lastResult as PageValidationResult.Valid
        assertEquals(20, lastValid.pageNumberOneBased)
        assertEquals(19, lastValid.targetIndexZeroBased)
    }

    @Test
    fun testValidPageWithWhitespacePadding() {
        val result = PageValidationResult.validate("  7  ", 15)
        assertTrue(result is PageValidationResult.Valid)
        assertEquals(7, (result as PageValidationResult.Valid).pageNumberOneBased)
        assertEquals(6, result.targetIndexZeroBased)
    }

    @Test
    fun testZeroAndNegativePageNumbersAreInvalid() {
        val totalPages = 10

        val zeroResult = PageValidationResult.validate("0", totalPages)
        assertTrue(zeroResult is PageValidationResult.Invalid)
        assertEquals("Page number must be 1 or greater.", (zeroResult as PageValidationResult.Invalid).message)

        val negativeResult = PageValidationResult.validate("-5", totalPages)
        assertTrue(negativeResult is PageValidationResult.Invalid)
    }

    @Test
    fun testExceedingTotalPagesIsInvalid() {
        val totalPages = 15

        val overResult = PageValidationResult.validate("16", totalPages)
        assertTrue(overResult is PageValidationResult.Invalid)
        assertEquals("Page number cannot exceed 15.", (overResult as PageValidationResult.Invalid).message)

        val largeOverResult = PageValidationResult.validate("9999", totalPages)
        assertTrue(largeOverResult is PageValidationResult.Invalid)
    }

    @Test
    fun testEmptyAndBlankInputsAreInvalid() {
        val emptyResult = PageValidationResult.validate("", 10)
        assertTrue(emptyResult is PageValidationResult.Invalid)
        assertEquals("Please enter a page number.", (emptyResult as PageValidationResult.Invalid).message)

        val blankResult = PageValidationResult.validate("   ", 10)
        assertTrue(blankResult is PageValidationResult.Invalid)
    }

    @Test
    fun testNonNumericInputsAreInvalid() {
        val lettersResult = PageValidationResult.validate("abc", 10)
        assertTrue(lettersResult is PageValidationResult.Invalid)
        assertEquals("Please enter numbers only.", (lettersResult as PageValidationResult.Invalid).message)

        val mixedResult = PageValidationResult.validate("12a", 10)
        assertTrue(mixedResult is PageValidationResult.Invalid)

        val symbolResult = PageValidationResult.validate("#$%", 10)
        assertTrue(symbolResult is PageValidationResult.Invalid)
    }

    @Test
    fun testSinglePageDocumentBoundaries() {
        val totalPages = 1

        val validOne = PageValidationResult.validate("1", totalPages)
        assertTrue(validOne is PageValidationResult.Valid)
        assertEquals(1, (validOne as PageValidationResult.Valid).pageNumberOneBased)
        assertEquals(0, validOne.targetIndexZeroBased)

        val invalidTwo = PageValidationResult.validate("2", totalPages)
        assertTrue(invalidTwo is PageValidationResult.Invalid)
    }
}
