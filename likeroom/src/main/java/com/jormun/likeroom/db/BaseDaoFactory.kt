package com.jormun.likeroom.db

import android.util.Log
import com.jormun.likeroom.base.IBaseDao
import com.jormun.likeroom.base.IClassParser
import com.jormun.likeroom.strategy.StrategyClass
import com.jormun.likeroom.strategy.StrategyWhere
import com.jormun.likeroom.utils.DbClassParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

//对外提供返回BaseDao的工厂类
object BaseDaoFactory {//为了方便调用做成单例吧

    //private lateinit var databaseFilePath: String

    //private lateinit var sqLiteDatabase: SQLiteDatabase

    private val mutex = Mutex()

    /**
     * 通过SQL Helper来获得Dao层
     */
    suspend fun <T : Any> getOrCreateBaseDao(
        sqlDatabaseHelper: BaseSQLDatabaseHelper,
        entityClass: Class<T>,
        dbClassParser: IClassParser
    ): BaseDao<T>? {
        mutex.withLock {//获取的过程中防止多次创建需要上锁
            try {
                val baseDao: BaseDao<T>
                if (DaoPool.getDaoFromPool(entityClass) != null) {//<-B
                    baseDao = DaoPool.getDaoFromPool(entityClass) as BaseDao<T>
                } else {
                    baseDao = BaseDao::class.java.newInstance() as BaseDao<T>
                    baseDao.initDao(sqlDatabaseHelper, entityClass, dbClassParser)//<-A
                    DaoPool.saveDaoToPool(entityClass, baseDao)
                }
                return baseDao
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }


    /**
     * @param daoClass: 泛型T，必须是BaseDao<*>的子类
     * @param entityClass: 泛型M的Class，也就是希望创建数据库的对象类。
     * @param baseSQLDatabaseHelper : 用来获得sqlitedatabase
     * 根据传入的对象entity，创建与之对应的BaseDao，并且初始化新建数据库
     * 假设需要有N个BaseDao的子类(T:BaseDao)进行功能拓展，同时这些子类BaseDao也必须分别属于各自对应的类型(M)，那么就可以用下面的方法生成。
     * 比如 BaseDao<User>，BaseDao<Student>， BaseDao<teacher>等等，这些里面又有各种不同的拓展方法，
     * 所以不能通过上面的统一方法生成，需要由这个方法来新建其实例进行生成。
     * 具体例子见主APP里面的 PersonDaoImpl
     *
     */
    suspend fun <T : BaseDao<M>, M> getExtensionDao(
        daoClass: Class<T>,
        entityClass: Class<M>,
        baseSQLDatabaseHelper: BaseSQLDatabaseHelper
    ): T {
        val baseDao = daoClass.newInstance()
        baseDao.initDao(baseSQLDatabaseHelper, entityClass, DbClassParser())
        return baseDao
    }

    /**
     * 通过动态代理的方式获得可以执行自己逻辑的扩展Dao
     *  @param context : 用来获取BaseDao
     *  @param entity : 用来获取BaseDao
     */
    suspend fun <T : Any> createBaseExtensionDao(
        entity: Class<T>,
        strategyClass: StrategyClass<T>,
        baseSQLDatabaseHelper: BaseSQLDatabaseHelper?
    ): IBaseDao<T>? {
        if (baseSQLDatabaseHelper == null) return null
        val baseDAo: BaseDao<T>? =
            getOrCreateBaseDao(baseSQLDatabaseHelper, entity, DbClassParser())
        baseDAo?.apply {
            val interfaces = baseDAo::class.java.interfaces
            val newProxyInstance = Proxy.newProxyInstance(
                IBaseDao::class.java.classLoader,
                interfaces,
                object : InvocationHandler {
                    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
                        runBlocking {
                            if (!strategyClass.isDoSuccess) {
                                when (strategyClass.where) {
                                    StrategyWhere.INSERT -> {
                                        strategyClass.isDoSuccess =
                                            strategyClass.beforeInsert(baseDAo)
                                    }
                                    StrategyWhere.DELETE -> {
                                        strategyClass.isDoSuccess =
                                            strategyClass.beforeDelete(baseDAo)
                                    }
                                    StrategyWhere.UPDATE -> {
                                        strategyClass.isDoSuccess =
                                            strategyClass.beforeUpdate(baseDAo)
                                    }
                                }
                            }
                            delay(2000)
                            Log.e("TAG", "invoke: 延迟2000毫秒完成！")
                        }
                        Log.e("TAG", "invoke: 调用invoke了！")
                        val invoke = method.invoke(
                            baseDAo,
                            *args.orEmpty()//***注意这个*args.orEmpty()！搞了一晚报错就因为这玩意！
                        )
                        /**
                         * 这里可以有一些after操作，暂时忽略
                         */
                        return invoke
                    }
                })
            return newProxyInstance as IBaseDao<T>
        }
        return null
    }

}