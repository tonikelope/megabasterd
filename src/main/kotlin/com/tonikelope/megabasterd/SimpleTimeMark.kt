package com.tonikelope.megabasterd

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A simple time mark that represents a point in time in milliseconds.
 *
 * @author DavidArthurCole
 */
@JvmInline
value class SimpleTimeMark(private val millis: Long) : Comparable<SimpleTimeMark> {

    operator fun minus(other: SimpleTimeMark) =
        (millis - other.millis).milliseconds

    operator fun plus(other: Duration) =
        SimpleTimeMark(millis + other.inWholeMilliseconds)

    operator fun minus(other: Duration) = plus(-other)

    override fun compareTo(other: SimpleTimeMark): Int = millis.compareTo(other.millis)

    override fun toString(): String = when (this) {
        farPast() -> "The Far Past"
        farFuture() -> "The Far Future"
        else -> Instant.ofEpochMilli(millis).toString()
    }

    companion object {

        fun now() = SimpleTimeMark(System.currentTimeMillis())

        @JvmStatic
        @JvmName("farPast")
        fun farPast() = SimpleTimeMark(0)
        fun farFuture() = SimpleTimeMark(Long.MAX_VALUE)
    }
}