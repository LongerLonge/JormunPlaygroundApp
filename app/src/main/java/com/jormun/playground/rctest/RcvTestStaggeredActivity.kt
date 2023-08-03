package com.jormun.playground.rctest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.jormun.playground.R

class RcvTestStaggeredActivity : AppCompatActivity() {
    private val TAG = "RcvTestStaggeredActivit"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rcv_test_staggered)
        initRecycleView()
    }

    private fun initRecycleView() {
        val fakeData = initFakeData(50)
        val rcv_staggered = findViewById<RecyclerView>(R.id.rcv_staggered)
        val staggeredGridLayoutManager = StaggeredGridLayoutManager(3, LinearLayout.VERTICAL)
        staggeredGridLayoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        rcv_staggered.layoutManager = staggeredGridLayoutManager
        /*val gridLayoutManager = GridLayoutManager(this, 3, RecyclerView.VERTICAL,false)
        rcv_staggered.layoutManager = gridLayoutManager*/
        val staggeredAdapter = StaggeredAdapter(this, fakeData)
        rcv_staggered.adapter = staggeredAdapter
        //rcv_staggered.addItemDecoration(SpaceItemDecoration(5))

    }

    private fun initFakeData(i: Int): List<FakeItem> {
        val items = ArrayList<FakeItem>()
        /*repeat(i) {
            val random = (0..1).random()
            items.add(FakeItem(random))
        }*/

        items.add(FakeItem(1))
        items.add(FakeItem(0))
        items.add(FakeItem(1))
        items.add(FakeItem(0))

        /*items.add(FakeItem(0))
        items.add(FakeItem(1))
        items.add(FakeItem(1))
        items.add(FakeItem(1))
        items.add(FakeItem(1))

        items.add(FakeItem(1))
        items.add(FakeItem(1))
        items.add(FakeItem(0))
        items.add(FakeItem(1))
        items.add(FakeItem(1))

        items.add(FakeItem(1))
        items.add(FakeItem(0))
        items.add(FakeItem(1))
        items.add(FakeItem(1))
        items.add(FakeItem(1))

        items.add(FakeItem(0))
        items.add(FakeItem(0))
        items.add(FakeItem(1))
        items.add(FakeItem(1))

        items.add(FakeItem(1))
        items.add(FakeItem(0))
        items.add(FakeItem(0))
        items.add(FakeItem(1))

        items.add(FakeItem(0))
        items.add(FakeItem(1))
        items.add(FakeItem(0))
        items.add(FakeItem(1))

        items.add(FakeItem(0))
        items.add(FakeItem(0))
        items.add(FakeItem(0))

        items.add(FakeItem(1))
        items.add(FakeItem(1))
        items.add(FakeItem(1))
        items.add(FakeItem(1))
        items.add(FakeItem(1))
        items.add(FakeItem(1))*/

        return items
    }
}