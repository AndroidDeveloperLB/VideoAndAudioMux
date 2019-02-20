package com.lb.videoandaudiomux

import android.view.View
import android.widget.ViewAnimator
import androidx.annotation.IdRes
import java.io.File
import java.io.InputStream

fun InputStream.toFile(file: File) = toFile(file.absolutePath)

fun InputStream.toFile(path: String) {
    use { input ->
        File(path).outputStream().use { input.copyTo(it) }
    }
}

fun ViewAnimator.setViewToSwitchTo(viewToSwitchTo: View, animate: Boolean = true): Boolean {
    if (currentView === viewToSwitchTo)
        return false
    for (i in 0 until childCount) {
        if (getChildAt(i) !== viewToSwitchTo)
            continue
        if (animate)
            displayedChild = i
        else {
            val outAnimation = this.outAnimation
            val inAnimation = this.inAnimation
            this.inAnimation = null
            this.outAnimation = null
            displayedChild = i
            this.inAnimation = inAnimation
            this.outAnimation = outAnimation
        }
        return true
    }
    return false
}

fun ViewAnimator.setViewToSwitchTo(@IdRes viewIdToSwitchTo: Int, animate: Boolean = true): Boolean {
    if (currentView.id == viewIdToSwitchTo)
        return false
    for (i in 0 until childCount) {
        if (getChildAt(i).id != viewIdToSwitchTo)
            continue
        if (animate)
            displayedChild = i
        else {
            val outAnimation = this.outAnimation
            val inAnimation = this.inAnimation
            this.inAnimation = null
            this.outAnimation = null
            displayedChild = i
            this.inAnimation = inAnimation
            this.outAnimation = outAnimation
        }
        return true
    }
    return false
}

object Utils {


}
