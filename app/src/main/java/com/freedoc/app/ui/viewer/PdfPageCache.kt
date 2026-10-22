package com.freedoc.app.ui.viewer

import android.graphics.Bitmap

/**
 * Generic bounded Least-Recently-Used (LRU) memory cache.
 *
 * Enforces strict memory budgeting:
 * - Evicts least-recently accessed entries when [maxEntries] is exceeded.
 * - Thread-safe access via internal synchronization.
 *
 * @param maxEntries Maximum number of entries to retain in memory simultaneously.
 */
class BoundedLruCache<K, V>(
    val maxEntries: Int = 8
) {
    init {
        require(maxEntries > 0) { "maxEntries must be greater than 0" }
    }

    private val lock = Any()
    private val map = object : LinkedHashMap<K, V>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxEntries
        }
    }

    val size: Int
        get() = synchronized(lock) { map.size }

    fun get(key: K): V? = synchronized(lock) {
        map[key]
    }

    fun put(key: K, value: V) = synchronized(lock) {
        map[key] = value
    }

    fun contains(key: K): Boolean = synchronized(lock) {
        map.containsKey(key)
    }

    fun snapshot(): Map<K, V> = synchronized(lock) {
        map.toMap()
    }

    fun clear() = synchronized(lock) {
        map.clear()
    }
}

/**
 * Type alias specializing [BoundedLruCache] for PDF page bitmaps.
 */
typealias PdfPageCache = BoundedLruCache<Int, Bitmap>
