package com.jormun.likeroom.db

/**
 * 用来缓存Dao实例的池子
 * 防止用户多次获得新实例导致表被覆盖
 */
object DaoPool {

    private val DaoMap = mutableMapOf<Class<*>, BaseDao<*>>()


    fun saveDaoToPool(entity: Class<*>, dao: BaseDao<*>) {
        DaoMap[entity] = dao
    }

    fun getDaoFromPool(entity: Class<*>): BaseDao<*>? {
        return DaoMap[entity]
    }

}