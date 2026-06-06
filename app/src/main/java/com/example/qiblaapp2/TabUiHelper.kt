package com.example.qiblaapp2

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

object TabUiHelper {
    /** Extra padding on bottom nav for gesture bar / system insets. */
    fun applyBottomNavInsets(activity: AppCompatActivity) {
        val bottomNav = activity.findViewById<View>(R.id.bottomNavigation) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = systemBottom)
            insets
        }
        ViewCompat.requestApplyInsets(bottomNav)
    }
}
