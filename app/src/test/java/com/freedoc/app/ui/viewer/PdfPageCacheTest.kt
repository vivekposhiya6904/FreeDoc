package com.freedoc.app.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PdfPageCacheTest {

    @Test
    fun testCacheInitialState() {
        val cache = BoundedLruCache<Int, String>(maxEntries = 3)
        assertEquals(0, cache.size)
        assertTrue(cache.snapshot().isEmpty())
    }

    @Test
    fun testPutAndGetWithinCapacity() {
        val cache = BoundedLruCache<Int, String>(maxEntries = 3)
        cache.put(0, "BitmapPage0")
        cache.put(1, "BitmapPage1")

        assertEquals(2, cache.size)
        assertTrue(cache.contains(0))
        assertTrue(cache.contains(1))
        assertFalse(cache.contains(2))

        assertEquals("BitmapPage0", cache.get(0))
        assertEquals("BitmapPage1", cache.get(1))
        assertNull(cache.get(2))
    }

    @Test
    fun testLruEvictionWhenCapacityExceeded() {
        // Cache with max capacity = 3
        val cache = BoundedLruCache<Int, String>(maxEntries = 3)
        cache.put(0, "Page0")
        cache.put(1, "Page1")
        cache.put(2, "Page2")
        assertEquals(3, cache.size)

        // Access Page0 to make Page1 the least recently used
        cache.get(0)

        // Put Page3 -> Page1 should be evicted!
        cache.put(3, "Page3")
        assertEquals(3, cache.size)

        assertTrue("Page0 was recently accessed, should be retained", cache.contains(0))
        assertFalse("Page1 was least recently used, should be evicted", cache.contains(1))
        assertTrue("Page2 should be retained", cache.contains(2))
        assertTrue("Page3 should be retained", cache.contains(3))
    }

    @Test
    fun testClearRemovesAllEntries() {
        val cache = BoundedLruCache<Int, String>(maxEntries = 5)
        cache.put(0, "Page0")
        cache.put(1, "Page1")
        assertEquals(2, cache.size)

        cache.clear()
        assertEquals(0, cache.size)
        assertFalse(cache.contains(0))
        assertFalse(cache.contains(1))
        assertTrue(cache.snapshot().isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidCapacityThrowsException() {
        BoundedLruCache<Int, String>(maxEntries = 0)
    }
}
