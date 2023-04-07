package com.jormun.likeroom

import com.jormun.likeroom.db.BaseDao
import com.jormun.likeroom.strategy.StrategyClass
import com.jormun.likeroom.strategy.StrategyWhere

class UserSeparateStrategy : StrategyClass<User>(StrategyWhere.INSERT) {

    override suspend  fun beforeInsert(bs: BaseDao<User>): Boolean {
        try {
            val queryList = bs.query(User())
            queryList?.let {
                for (user in it) {
                    if (user.statue != 0) {
                        val where = User(user.id)
                        user.statue = 0
                        bs.update(user, where)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override suspend  fun afterInsert(bs: BaseDao<User>): Boolean {
        return true
    }
}