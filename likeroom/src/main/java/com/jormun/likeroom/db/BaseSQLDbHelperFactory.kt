package com.jormun.likeroom.db

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BaseSQLDbHelperFactory {

    private val mutex = Mutex()

    //private val

    suspend fun <T : Any> getBaseSQLDBHelper(
        context: Context?,
        dbName: String
    ): BaseSQLDatabaseHelper {
        mutex.withLock {//锁住防止多次创建
            var sqlDbHelper = SQLHelperPool.getSQLHelperFromPool(dbName)
            if (sqlDbHelper == null) {
                sqlDbHelper = BaseSQLDatabaseHelper(context, dbName, 1)
                SQLHelperPool.saveSQLHelperToPool(dbName, sqlDbHelper)
            }
            return sqlDbHelper
        }

    }
}