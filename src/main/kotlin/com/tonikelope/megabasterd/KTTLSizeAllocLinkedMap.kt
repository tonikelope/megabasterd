package com.tonikelope.megabasterd

import java.util.function.Function
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A LinkedHashMap that supports time-to-live (TTL) for its entries and a maximum size.
 * Entries are evicted based on their last access time and the specified TTL.
 *
 * @param T The type of values stored in the map.
 *
 * @author DavidArthurCole
 */
@Suppress("UNCHECKED_CAST")
class KTTLSizeAllocLinkedMap<T> : LinkedHashMap<Int?, T?>() {
    companion object {
        private val CACHE_TTL: Duration = 20.seconds
        private const val CACHE_MAX_SIZE = 100
    }

    // Internal cache entry class with timestamp
    private class CacheEntry<T>(var data: T) {
        var lastAccessTime: SimpleTimeMark = SimpleTimeMark.now()

        fun updateAccessTime() {
            this.lastAccessTime = SimpleTimeMark.now()
        }

        val isExpired: Boolean
            get() = SimpleTimeMark.now() - lastAccessTime > CACHE_TTL
    }

    override fun removeEldestEntry(eldest: Map.Entry<Int?, T?>): Boolean =
        if (eldest.value is CacheEntry<*>) {
            val entry = eldest.value as CacheEntry<T>
            // Evict if cache size exceeds the max size or if the entry is expired
            size > CACHE_MAX_SIZE || entry.isExpired
        } else false

    override fun computeIfAbsent(key: Int?, mappingFunction: Function<in Int?, out T?>): T? {
        // Check if the entry is present and not expired
        var existing = super.get(key)
        if (existing is CacheEntry<*>) {
            val entry = existing as CacheEntry<T>
            if (entry.isExpired) {
                remove(key)
                existing = null
            } else entry.updateAccessTime()
        }

        // If not found or expired, compute and insert new value
        if (existing == null) {
            val value = mappingFunction.apply(key)
            val newEntry = CacheEntry(value)
            super.put(key, newEntry as T)
            return value
        }
        return existing
    }

    override fun get(key: Int?): T? {
        val entry = super.get(key).takeIf { it is CacheEntry<*> } as? CacheEntry<T> ?: return null
        entry.updateAccessTime()
        return if (entry.isExpired) {
            super.remove(key)
            null
        } else entry.data
    }
}
