package com.tonikelope.megabasterd

import java.io.InputStream
import java.lang.System.nanoTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

open class KThrottledInputStream(
    private val rawStream: InputStream,
    private val streamSupervisor: StreamThrottlerSupervisor
) : InputStream() {

    private val streamFinished = AtomicBoolean(false)

    private var availableBytes: Long = 0
    private var lastRefillTime: Long = nanoTime()

    override fun read(): Int {
        if (streamSupervisor.maxBytesPerSecInput > 0) {
            if (streamFinished.get()) return -1
            throttle(1)
            val r = rawStream.read()
            if (r == -1) streamFinished.set(true)
            return r
        }
        val r = rawStream.read()
        if (r == -1) streamFinished.set(true)
        return r
    }

    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (streamSupervisor.maxBytesPerSecInput > 0) {
            if (streamFinished.get()) return -1
            throttle(len)
            val readLen = rawStream.read(b, off, len)
            if (readLen == -1) {
                streamFinished.set(true)
            }
            return readLen
        }
        return rawStream.read(b, off, len)
    }

    override fun reset() {
        streamFinished.set(false)
        rawStream.reset()
    }

    private fun throttle(requested: Int) {
        val maxRate = streamSupervisor.maxBytesPerSecInput.toLong().takeIf { it > 0 } ?: return
        refillTokens(maxRate)

        synchronized(this) {
            while (availableBytes < requested) {
                val nanosToWait = ((requested - availableBytes) * 1_000_000_000L) / maxRate
                if (nanosToWait > 0) {
                    try {
                        Thread.sleep(nanosToWait / 1_000_000, (nanosToWait % 1_000_000).toInt())
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
                refillTokens(maxRate)
            }
            availableBytes -= min(availableBytes, requested.toLong())
        }
    }

    private fun refillTokens(maxRate: Long) {
        val now = nanoTime()
        val elapsedNanos = now - lastRefillTime
        if (elapsedNanos > 0) {
            val newTokens = (elapsedNanos * maxRate) / 1_000_000_000L
            if (newTokens > 0) {
                availableBytes = min(maxRate, availableBytes + newTokens)
                lastRefillTime = now
            }
        }
    }
}