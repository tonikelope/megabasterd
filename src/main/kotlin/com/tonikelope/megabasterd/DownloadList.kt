package com.tonikelope.megabasterd

import org.jdesktop.swingx.JXList
import javax.swing.DefaultListModel
import javax.swing.ListCellRenderer

class DownloadList : JXList() {
    private val model = DefaultListModel<DownloadView>()

    init {
        setModel(model)

        cellRenderer = ListCellRenderer<DownloadView> { list, value, _, isSelected, _ ->
            value.apply {
                background = if (isSelected) list.selectionBackground else list.background
                foreground = if (isSelected) list.selectionForeground else list.foreground
            }
        }

        visibleRowCount = -1
        fixedCellHeight = -1
        fixedCellWidth = -1
    }

    fun addDownload(download: Download) {
        model.addElement(download.view)
    }

    fun removeDownload(download: Download) {
        model.removeElement(download.view)
    }
}

