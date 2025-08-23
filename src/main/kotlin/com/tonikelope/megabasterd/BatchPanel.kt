package com.tonikelope.megabasterd

import javax.swing.JPanel

class BatchPanel : JPanel() {
    private var suspendLayout = false

    fun setSuspendLayout(suspend: Boolean) {
        suspendLayout = suspend
    }

    override fun doLayout() {
        if (!suspendLayout) super.doLayout()
    }
}