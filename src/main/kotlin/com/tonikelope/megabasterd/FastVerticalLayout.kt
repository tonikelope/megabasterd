package com.tonikelope.megabasterd

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Insets
import java.awt.LayoutManager

class FastVerticalLayout(private val gap: Int = 0) : LayoutManager {

    override fun addLayoutComponent(name: String?, comp: Component?) { }

    override fun removeLayoutComponent(comp: Component?) { }

    override fun preferredLayoutSize(parent: Container): Dimension {
        synchronized(parent.treeLock) {
            var width = 0
            var height = 0

            for (comp in parent.components) {
                if (!comp.isVisible) continue
                val d = comp.preferredSize
                width = maxOf(width, d.width)
                height += d.height + gap
            }

            val insets: Insets = parent.insets
            return Dimension(
                width + insets.left + insets.right,
                height + insets.top + insets.bottom
            )
        }
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        return preferredLayoutSize(parent)
    }

    override fun layoutContainer(parent: Container) {
        synchronized(parent.treeLock) {
            val insets: Insets = parent.insets
            var y = insets.top
            val x = insets.left
            val width = parent.width - insets.left - insets.right

            for (comp in parent.components) {
                if (!comp.isVisible) continue
                val d = comp.preferredSize
                comp.setBounds(x, y, width, d.height)
                y += d.height + gap
            }
        }
    }
}
