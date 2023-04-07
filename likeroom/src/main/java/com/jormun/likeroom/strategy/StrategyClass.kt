package com.jormun.likeroom.strategy

import com.jormun.likeroom.db.BaseDao

abstract class StrategyClass<T : Any>(val where: StrategyWhere) : StrategyInterface<T> {
    var isDoSuccess = false

    override suspend  fun beforeInsert(bs: BaseDao<T>): Boolean {
        return false
    }

    override suspend  fun afterInsert(bs: BaseDao<T>): Boolean {
        return false
    }

    override suspend  fun beforeUpdate(bs: BaseDao<T>): Boolean {
        return false
    }

    override suspend  fun afterUpdate(bs: BaseDao<T>): Boolean {
        return false
    }

    override suspend  fun beforeDelete(bs: BaseDao<T>): Boolean {
        return false
    }

    override suspend  fun afterDelete(bs: BaseDao<T>): Boolean {
        return false
    }
}