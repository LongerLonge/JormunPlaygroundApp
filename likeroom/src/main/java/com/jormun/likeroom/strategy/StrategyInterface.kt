package com.jormun.likeroom.strategy

import com.jormun.likeroom.db.BaseDao

interface StrategyInterface<T : Any> {

    suspend fun  beforeInsert(bs: BaseDao<T>): Boolean
    suspend fun afterInsert(bs: BaseDao<T>): Boolean
    suspend fun beforeDelete(bs: BaseDao<T>): Boolean
    suspend fun afterDelete(bs: BaseDao<T>): Boolean
    suspend fun beforeUpdate(bs: BaseDao<T>): Boolean
    suspend fun afterUpdate(bs: BaseDao<T>): Boolean
}