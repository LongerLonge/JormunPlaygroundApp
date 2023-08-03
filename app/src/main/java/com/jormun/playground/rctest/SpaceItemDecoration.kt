package com.jormun.playground.rctest

import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration(private val sapce: Int) : RecyclerView.ItemDecoration() {

    init {
        //TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,5.0,)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = sapce
        outRect.right = sapce
        outRect.top = sapce
        outRect.bottom = sapce
    }

}