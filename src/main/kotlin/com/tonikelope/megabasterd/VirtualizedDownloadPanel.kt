package com.tonikelope.megabasterd

import javax.swing.BoxLayout
import javax.swing.JPanel

class VirtualizedDownloadPanel(
    private val downloads: MutableList<Download> = mutableListOf()
) : JPanel() {
    private val visibleViews = mutableMapOf<Int, DownloadView>()
    private var lastStartIndex = 0
    private var lastEndIndex = 20

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    private fun checkDataInit() {
        if (visibleViews.isEmpty()) updateVisibleRows(lastStartIndex, lastEndIndex)
    }

    fun getPrefHeight(): Int =
        downloads.firstOrNull()?.view?.preferredSize?.height ?: DownloadView.DEFAULT_ROW_HEIGHT

    fun getDownloadCount(): Int = downloads.size

    fun addDownload(download: Download) {
        downloads.add(download)
        checkDataInit()
        val index = downloads.lastIndex
        if (index in lastStartIndex..lastEndIndex) {
            repaintRows(lastStartIndex, lastEndIndex)
        }
    }

    fun removeDownload(download: Download) {
        checkDataInit()
        val index = downloads.indexOf(download)
        if (index != -1) {
            downloads.removeAt(index)
            visibleViews.remove(index)

            val shifted = visibleViews.entries.filter { it.key > index }
                .associate { (oldIndex, view) -> (oldIndex - 1) to view }

            visibleViews.keys.removeAll { it > index }
            visibleViews.putAll(shifted)

            repaintRows(lastStartIndex, lastEndIndex)
        }
    }

    fun updateVisibleRows(startIndex: Int, endIndex: Int) {
        lastStartIndex = startIndex
        lastEndIndex = endIndex

        val paddedStartIndex = (startIndex - 5).coerceAtLeast(0)
        val paddedEndIndex = (endIndex + 5).coerceAtMost(downloads.size - 1)

        val toRemove = visibleViews.keys.filter { it < paddedStartIndex || it > paddedEndIndex }
        for (i in toRemove) {
            remove(visibleViews.remove(i))
        }

        repaintRows(paddedStartIndex, paddedEndIndex)
    }

    private fun repaintRows(startIndex: Int, endIndex: Int) {
        val toRemove = visibleViews.keys.filter { it < startIndex || it > endIndex }
        for (i in toRemove) {
            val view = visibleViews.remove(i)
            if (view != null) remove(view)
        }

        for (i in startIndex..endIndex) {
            if (!visibleViews.containsKey(i)) {
                val view = downloads[i].view
                visibleViews[i] = view
                add(view, i - startIndex)
            } else {
                val view = visibleViews[i]
                val currentIndex = components.indexOf(view)
                val desiredIndex = i - startIndex
                if (currentIndex != desiredIndex) {
                    remove(view)
                    add(view, desiredIndex)
                }
            }
        }

        revalidate()
        repaint()
    }
}
