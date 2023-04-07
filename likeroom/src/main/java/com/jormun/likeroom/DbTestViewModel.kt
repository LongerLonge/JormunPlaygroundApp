package com.jormun.likeroom

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jormun.likeroom.db.BaseDaoFactory
import com.jormun.likeroom.db.BaseSQLDatabaseHelper
import com.jormun.likeroom.db.BaseSQLDbHelperFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class DbTestViewModel : ViewModel() {

    private val uLd = MutableLiveData<List<User>>()
    private lateinit var weakContext: WeakReference<Context>


    private suspend fun getSQLDbHelper(dbName: String, context: Context?): BaseSQLDatabaseHelper? {
        if (context == null) return null
        val userSQLDbHelper = BaseSQLDbHelperFactory.getBaseSQLDBHelper<User>(
            context,
            dbName
        )

        userSQLDbHelper.setTableEntityAndInitDao(User::class.java)
        return userSQLDbHelper
    }

    fun createDB(context: Context) {
        weakContext = WeakReference(context)
        GlobalScope.launch {
            getSQLDbHelper("user", context)
            //Log.e("TAG", "cache dir path: ${this.dataDir.absolutePath}")
        }.start()
    }

    fun insertDataToDB(context: Context) {
        GlobalScope.launch {
            val userSQLDbHelper = getSQLDbHelper("user", context)
            userSQLDbHelper?.insert(User(1, "jack", "123", 0))
            userSQLDbHelper?.insert(User(2, "andy", "222", 1))
            userSQLDbHelper?.insert(User(3, "Foss", "333", 0))
        }.start()
    }

    fun updateToDB(context: Context) {
        GlobalScope.launch {
            val userSQLDbHelper = getSQLDbHelper("user", context)
            val newUser = User(null, null, "777")
            val where = User(1, "jack")
            userSQLDbHelper?.update(newUser, where)
        }.start()
    }


    fun deleteDataFromDB(context: Context) {
        GlobalScope.launch {
            val userSQLDbHelper = getSQLDbHelper("user", context)
            val deleteUser = User(3)
            userSQLDbHelper?.delete(deleteUser)
        }.start()
    }

    fun queryFromDB(context: Context): LiveData<List<User>> {
        GlobalScope.launch {
            val userSQLDbHelper = getSQLDbHelper("user", context)
            val query = userSQLDbHelper?.query(User())
            uLd.postValue(query)
        }.start()
        return uLd
    }

    fun doSeparateOperation(context: Context) {
        GlobalScope.launch {
            val baseSQLDBHelper = getSQLDbHelper("user", context)
            val userSeparateDao =
                BaseDaoFactory.createBaseExtensionDao(
                    User::class.java,
                    UserSeparateStrategy(),
                    baseSQLDBHelper
                )
            userSeparateDao?.insert(User(1, "jack", null, 1))
        }.start()
    }

    fun doCheckExitsInsert(context: Context) {
        GlobalScope.launch {
            val baseSQLDBHelper = getSQLDbHelper("user", context)
            baseSQLDBHelper?.insert(User(1, "jack", "111"))
            //Log.e("TAG", "cache dir path: ${this.dataDir.absolutePath}")
        }.start()
    }

    fun doUpdateDbToCreate(context: Context) {
        //val baseSQLDBHelper = BaseSQLDbHelperFactory.getBaseSQLDBHelper<User>(this, "user")
        //baseSQLDBHelper.closeAllLink()
        //注意升级前，一定要close所有db！！！！！
        GlobalScope.launch {
            val sqlDbHelper = getSQLDbHelper("user", context)
        }
    }


}