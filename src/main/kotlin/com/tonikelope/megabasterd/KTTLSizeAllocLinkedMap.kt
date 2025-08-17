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
    private val backingCache = object : LinkedHashMap<Int?, CacheEntry<T?>>(CACHE_MAX_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Int?, CacheEntry<T?>>): Boolean {
            return size > CACHE_MAX_SIZE || eldest.value.isExpired
        }
    }

    companion object {
        private val CACHE_TTL: Duration = 20.seconds
        private const val CACHE_MAX_SIZE = 100
    }

    class CacheEntry<T>(var data: T) {
        private var lastAccessTime: SimpleTimeMark = SimpleTimeMark.now()

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
        var existing = backingCache[key]
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
            backingCache[key] = newEntry
            return value
        }
        return existing.data
    }

    override fun get(key: Int?): T? {
        val entry = backingCache[key] ?: return null
        entry.updateAccessTime()
        return if (entry.isExpired) {
            backingCache.remove(key)
            null
        } else entry.data
    }
}
