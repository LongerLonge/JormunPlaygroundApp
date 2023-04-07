package com.jormun

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jormun.playground.rftest.ApiServersTest
import com.jormun.retrofit.RetrofitMock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val rfLD = MutableLiveData<String>()

    fun testRf(): LiveData<String> {
        viewModelScope.launch(Dispatchers.IO) {
            val retrofitMock = RetrofitMock("https://kuaidi100.com")
            val create = retrofitMock.create(ApiServersTest::class.java)
            val call = create.getKuaidiFromServer("1001")
            val execute = call.enqueue()
            Log.e("MainTAG", "run: $execute")
            rfLD.postValue(execute)
        }
        return rfLD
    }
}