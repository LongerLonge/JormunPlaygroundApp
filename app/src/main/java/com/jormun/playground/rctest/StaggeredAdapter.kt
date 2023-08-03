package com.jormun.playground.rctest

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.jormun.playground.R

class StaggeredAdapter(val context: Context, val itemList: List<FakeItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        return StagViewHolder(
            view = when (viewType) {
                0 -> {
                    layoutInflater.inflate(R.layout.stag_big_item, parent, false)
                }

                else -> {
                    layoutInflater.inflate(R.layout.stag_small_item, parent, false)
                }
            }
        )
    }


    override fun getItemViewType(position: Int): Int {
        return itemList[position].type
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val stagViewHolder = holder as StagViewHolder
        stagViewHolder.tv_.text = "$position"
    }

    class StagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tv_ = view.findViewById<TextView>(R.id.tv_)
    }
}