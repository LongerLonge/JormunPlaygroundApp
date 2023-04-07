package com.jormun.likeroom.db

object SQLHelperPool {
    private val pool = mutableMapOf<String, BaseSQLDatabaseHelper>()

    fun getSQLHelperFromPool(dbName: String): BaseSQLDatabaseHelper? {
        return pool[dbName]
    }

    fun saveSQLHelperToPool(dbName: String, sqlDatabaseHelper: BaseSQLDatabaseHelper) {
        pool[dbName] = sqlDatabaseHelper
    }
}