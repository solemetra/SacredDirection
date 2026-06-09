package com.example.qiblaapp2

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

object TabUiHelper {

    enum class BottomTab {
        DIRECTION, PRAYER, DUA, SETTINGS
    }

    private const val INACTIVE_ALPHA = 0.72f
    private const val ACTIVE_SCALE = 1.08f

    /** Floating pill: lift bottom margin above gesture bar / system insets. */
    fun applyBottomNavInsets(activity: AppCompatActivity) {
        val bottomNav = activity.findViewById<View>(R.id.bottomNavigation) ?: return
        val baseMargin =
            activity.resources.getDimensionPixelSize(R.dimen.bottom_nav_floating_margin_bottom)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = baseMargin + systemBottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(bottomNav)
    }

    fun highlightBottomTab(activity: AppCompatActivity, active: BottomTab) {
        val tabs = listOf(
            BottomTab.DIRECTION to R.id.iconDirection,
            BottomTab.PRAYER to R.id.iconPrayer,
            BottomTab.DUA to R.id.iconDua,
            BottomTab.SETTINGS to R.id.iconSettings,
        )

        for ((tab, iconId) in tabs) {
            val icon = activity.findViewById<ImageView>(iconId) ?: continue
            val isActive = tab == active
            icon.alpha = if (isActive) 1f else INACTIVE_ALPHA
            val scale = if (isActive) ACTIVE_SCALE else 1f
            icon.scaleX = scale
            icon.scaleY = scale
        }
    }
}
