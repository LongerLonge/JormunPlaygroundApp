package com.jormun.likeroom.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.jormun.likeroom.utils.DbClassParser
import com.jormun.likeroom.utils.TAGS.SQLTAG

/**
 *负责帮忙管理数据库的类
 * 负责打开并且返回数据库，建表和升级等操作暂时不由这个类提供
 */
class BaseSQLDatabaseHelper(val context: Context?, val dbName: String, val version: Int) :
    SQLiteOpenHelper(context, dbName, null, version) {

    private var writeDb: SQLiteDatabase? = null
    private var readDb: SQLiteDatabase? = null

    private val tableClassList = arrayListOf<Class<*>>()

    suspend fun <T : Any> setTableEntityAndInitDao(entity: Class<T>) {
        val cacheDao = DaoPool.getDaoFromPool(entity)
        if (!tableClassList.contains(entity)) {
            if (cacheDao == null) {
                tableClassList.add(entity)
                BaseDaoFactory.getOrCreateBaseDao(this, entity, DbClassParser())
            }
        }
    }


    fun openWriteDbLink(): SQLiteDatabase? {
        if (writeDb == null || !writeDb!!.isOpen) {
            writeDb = writableDatabase
            Log.e(SQLTAG, "db path is : ${writableDatabase.path}")
        }
        return writeDb
    }

    fun openReadDbLink(): SQLiteDatabase? {
        if (readDb == null || !readDb!!.isOpen) {
            readDb = readableDatabase
            Log.e(SQLTAG, "db path is : ${readableDatabase.path}")
        }
        return readDb
    }

    //关闭联机并置空
    fun closeWriteLink() {
        writeDb?.apply {
            if (isOpen) close()
            writeDb = null
        }
    }

    fun closeReadLink() {
        readDb?.apply {
            if (isOpen) close()
            readDb = null
        }
    }

    fun closeAllLink() {
        closeReadLink()
        closeWriteLink()
    }

    /**
     * 插入
     */
    suspend fun <T : Any> insert(entity: T?): Long {
        val entityDao = getEntityDao(entity) ?: return -1
        return entityDao.insert(entity)
    }

    /**
     * 更新
     */
    suspend fun <T : Any> update(entity: T?, where: T?): Int {
        val entityDao = getEntityDao(entity) ?: return -1
        return entityDao.update(entity, where)
    }

    /**
     * 删除
     */
    suspend fun <T : Any> delete(entity: T?): Int {
        val entityDao = getEntityDao(entity) ?: return -1
        return entityDao.delete(entity)
    }

    /**
     * 查询，简单版
     */
    suspend fun <T : Any> query(entity: T?): List<T>? {
        val entityDao = getEntityDao(entity) ?: return null
        return entityDao.query(entity)
    }


    private fun <T : Any> getEntityDao(entity: T?): BaseDao<T>? {
        if (entity == null) {
            Log.e(SQLTAG, "insert err: entity not null!")
            return null
        }
        val cacheDao = DaoPool.getDaoFromPool(entity::class.java)
        if (cacheDao == null) {
            Log.e(SQLTAG, "insert err: dao is null, please init dao first!")
            return null
        }
        return cacheDao as BaseDao<T>
    }


    //因为我们是通过类创建来创建表，所以对于数据库而言，一开始就是空白不需要任何表，直到需要用到才让Dao层自己建表
    //我称之为，分布式建表！哈哈
    override fun onCreate(db: SQLiteDatabase?) {
        //数据库的表创建暂时不让这个类处理
        Log.e("TAG", "db onCreate: ${db?.path}")
    }

    //跟Dao负责自己创建表一样，升级直接抛给Dao来自己升级就好了
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //数据库的升级暂时不让这个类处理
    }
}