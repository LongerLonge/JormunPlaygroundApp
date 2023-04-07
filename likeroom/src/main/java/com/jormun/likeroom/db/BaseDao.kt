package com.jormun.likeroom.db

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.jormun.likeroom.an.DbField
import com.jormun.likeroom.base.IBaseDao
import com.jormun.likeroom.base.IClassParser
import com.jormun.likeroom.update.UpdateDBManager
import com.jormun.likeroom.utils.TAGS.DAOTAG
import java.lang.reflect.Field

//对外提供的就是这个BaseDao，提供增删改查等操作
//构造私有，同时让用户指定相应的泛型


open class BaseDao<T : Any> : IBaseDao<T> {

    private lateinit var sqlDatabaseHelper: BaseSQLDatabaseHelper//用来获取数据库

    private lateinit var dbClassParser: IClassParser//class类的解析对象

    private lateinit var tableName: String//表名

    private lateinit var entityClass: Class<T>//需要解析的类class

    private val columnFieldMap = mutableMapOf<String, Field>()//缓存Map，缓存 列名->Field

    private var isInit = false//标志位，是否已经初始化过

    private lateinit var mainKeyName: String//记录该表里面的主键


    /**
     * 对Dao类进行初始化
     * @param sqlDatabaseHelper : 获取database进行操作的helper
     * @param entityClass : 需要解析的类，也就是需要操作的表
     * @param dbClassParser : 类解析器
     */
    suspend fun initDao(
        sqlDatabaseHelper: BaseSQLDatabaseHelper?,
        entityClass: Class<T>?,
        dbClassParser: IClassParser?
    ) {
        if (entityClass == null || sqlDatabaseHelper == null || dbClassParser == null) {
            Log.e(DAOTAG, "initDao err: entity or sqlDatabaseHelper or dbClassParser is not null!")
            return
        }

        this.sqlDatabaseHelper = sqlDatabaseHelper
        this.entityClass = entityClass
        this.dbClassParser = dbClassParser

        if (!isInit) {
            //通过注解自动获得表名，要是没有注解就直接获得类名作为表名
            /*tableName = entityClass.getAnnotation(DbTable::class.java)?.dbname ?: run {
                entityClass.simpleName
            }*/
            //1.取得表名
            tableName = dbClassParser.getTableNameFromEntity(entityClass)

            //2.创建表
            val createTableSQL = getCreateTableSQL()
            Log.e(DAOTAG, "initDao: $createTableSQL")
            val writeDb = this.sqlDatabaseHelper.openWriteDbLink()
            if (writeDb == null) {
                Log.e(DAOTAG, "initDao: get Db is null!")
                return
            }
            writeDb.execSQL(createTableSQL)
            //3.检查数表是否需要更新
            val updateIsSuccess = updateTableVersion(
                sqlDatabaseHelper.context,
                createTableSQL,
                tableName,
                sqlDatabaseHelper
            )
            //4.初始化缓存Map
            initColumnFieldMap()
            isInit = true && updateIsSuccess
        }
    }

    /**
     * 检查是否需要更新表，从本地的xml文件中获取
     * @param context :因为更新需要备份数据库因此涉及到复制和粘贴操作，需要获取路径
     * @param createNewTableSql : 创建Table的sql语句，用来进行table更新
     * @param tableName : 表名
     * @param sqlDatabaseHelper : 用来获取database执行sql语句
     */
    private suspend fun updateTableVersion(
        context: Context?,
        createNewTableSql: String,
        tableName: String,
        sqlDatabaseHelper: BaseSQLDatabaseHelper
    ): Boolean {
        if (context == null) return false
        val finTableName = "${tableName}Update.xml"
        return UpdateDBManager(context).doDataBaseUpdate(
            createNewTableSql,
            finTableName,
            sqlDatabaseHelper
        )
    }

    //负责对class解析并且拼接好sql语句返回
    private fun getCreateTableSQL(): String {
        // 类似于要拼接出下面这条指令
        // create table if not exists
        // tb_user(_id INTEGER, name TEXT, password TEXT)
        return buildString {
            append("create table if not exists $tableName(")
            //根据成员变量的类型来拼接剩下的语句
            for (field in entityClass.declaredFields) {
                //加了注解的直接拿出里面的filedName，没有的直接拿出里面的filedName
                val filedName = field.getAnnotation(DbField::class.java)?.fieldName ?: run {
                    field.name
                }

                //先判断类型
                val typeName: String = when (field.type) {
                    Integer::class.java -> " INTEGER"
                    String::class.java -> " TEXT"
                    Long::class.java -> " BIGINT"
                    Double::class.java -> " DOUBLE"
                    else -> ""//暂时忽略容错
                }
                append("$filedName$typeName,")
            }
            //循环结束后，去掉末尾的 “,”
            deleteCharAt(this.length - 1)
            append(")")
        }
    }

    //初始化KV缓存Map
    private fun initColumnFieldMap() {
        //获取所有的字段名：
        val sql = "select * from $tableName limit 1, 0"
        val readDb = sqlDatabaseHelper.openReadDbLink()
        if (readDb == null) {
            Log.e(DAOTAG, "initCacheMap: get read db null! ")
            return
        }
        mainKeyName = dbClassParser.getMainKeyFromEntity(entityClass)
        val rawQueryCursor = readDb.rawQuery(sql, null)//通过空查询获得指针
        val columnNames = rawQueryCursor.columnNames//获得列名(也就是字段名)
        val cFMap = dbClassParser.parseColumFieldMap(entityClass, columnNames)
        if (cFMap == null) {
            //mainKeyName = dbClassParser.mainKeyName
            Log.e(DAOTAG, "initCacheMap err: column field map is null.")
        } else columnFieldMap.putAll(cFMap)
        rawQueryCursor.close()//!!!切记要关闭
    }

    /**
     * @param entity : 需要写入到库里的对象
     */
    override suspend fun insert(entity: T?): Long {//user(1,jack,222,0)
        if (entity == null) {
            Log.e(DAOTAG, "insert err: entity is null!")
            return -1
        }
        //获取键值对的所有值：
        val keyValues = dbClassParser.getFieldValueMap(entity, columnFieldMap.values.toList())
        val contentValues = getContentValues(keyValues)
        //需要判断是否已经存在，再执行插入操作！
        val mainKeyEntity =
            dbClassParser.makeMainKeyEntity(entity, columnFieldMap[mainKeyName]) ?: return -1
        val isItemExist = findIsItemExist(mainKeyEntity)
        if (!isItemExist) {
            val writeDb = sqlDatabaseHelper.openWriteDbLink()
            if (writeDb == null) {
                Log.e(DAOTAG, "insert: write db is null.")
                return -1
            }
            return writeDb.insert(tableName, null, contentValues)
        } else {
            return update(entity, mainKeyEntity).toLong()
        }
    }

    /**
     * 查找要存入的对象是否已经存在
     */
    private suspend fun findIsItemExist(where: T): Boolean {
        //val where = dbClassParser.makeMainKeyEntity(entity, mainKeyName)
        val query = query(where)
        query?.apply {
            if (isNotEmpty())
                return true
        }
        return false
    }

    /**
     * 把解析好后的成员变量或者注解Map转换成ContentValues
     * @param keyValues : 解析好的成员变量或者注解Map
     */
    private fun getContentValues(keyValues: Map<String, String?>): ContentValues {//把键值对拼接成ContentValues返回
        val contentValues = ContentValues()
        for (kv in keyValues) {
            contentValues.put(kv.key, kv.value)
        }
        return contentValues
    }

    /**
     * @param entity : 需要写入到库进行修改的数据
     * @param where : 条件对象
     */
    override suspend fun update(entity: T?, where: T?): Int {
        //更新数据
        //tableName=表名，contentValues=需要写入的字段和值，“name=?”=条件字段和占位符，最后的数组代表条件具体的值
        //sqLiteDataBase.update(tableName,contentValues,"name = ? and password = ?", arrayOf("haha","7777"))
        //上面那句代码代表，修改name=haha的这条数据

        var result = -1

        if (where == null || entity == null) {
            Log.e("TAG", "update: where not null!")
            return result
        }

        //先把传入进来的entity解析出来，并且转换成 ContentValues
        val keyValuesMap = dbClassParser.getFieldValueMap(entity, columnFieldMap.values.toList())
        val contentValues = getContentValues(keyValuesMap)

        //条件Map：
        val whereKVMap = dbClassParser.getFieldValueMap(where, columnFieldMap.values.toList())

        val condition = Condition(whereKVMap)
        val writeDb = sqlDatabaseHelper.openWriteDbLink()
        if (writeDb == null) {
            Log.e(DAOTAG, "update: get write db is null!")
            return result
        }
        result = writeDb.update(
            tableName,
            contentValues,
            condition.getWhereClauseSafe(),
            condition.getWhereArgsSafe()
        )

        return result
    }

    /**
     * 从数据库中删除
     * @param entity : 需要删除的数据
     */
    override suspend fun delete(entity: T?): Int {
        //sqLiteDataBase.delete(tableName,"_id=?","1")
        if (entity == null) {
            Log.e(DAOTAG, "delete err: entity is null!")
            return -1
        }
        val keyValuesMap = dbClassParser.getFieldValueMap(entity, columnFieldMap.values.toList())
        val con = Condition(keyValuesMap)
        val writeDb = sqlDatabaseHelper.openWriteDbLink()
        if (writeDb == null) {
            Log.e(DAOTAG, "delete: get db is null!")
            return -1
        }
        return writeDb
            .delete(tableName, con.getWhereClauseSafe(), con.getWhereArgsSafe())
    }

    /**
     * 简单不带参数的查询
     * @param entity : 需要查询的实例，里面包含条件
     */
    override suspend fun query(entity: T?): List<T>? {
        return query(entity, null, null, null)
    }

    /**
     * 带参数的高级查询
     * @param entity: 实例，包含条件
     * @param order: 是否需要排序等
     * @param startIndex: 从哪里开始
     * @param limit: 一次查多少
     */
    override suspend fun query(
        entity: T?,
        order: String?,
        startIndex: Int?,
        limit: Int?
    ): List<T>? {
        //sqLiteDataBase.query(tableName, null, "_id = ?", arrayOf(), null, null, order, limit)

        //把对象解析成KV Map = [id -> 1, name -> andy, ...]
        if (entity == null) {
            Log.e("TAG", "query: entity not null!")
            return null
        }
        val keyValuesMap = dbClassParser.getFieldValueMap(entity, columnFieldMap.values.toList())
        //解析条件 (_id = ? and name = ? and ....) ["1", "andy", ...]
        val con = Condition(keyValuesMap)

        //拼接limit
        var limitStr = ""
        if (startIndex != null && limit != null) {
            limitStr = "$startIndex , $limit"
        }
        val readDb = sqlDatabaseHelper.openReadDbLink()
        if (readDb == null) {
            Log.e(DAOTAG, "query: get db is null!")
            return null
        }

        //通过sql把游标查询出来
        val query = readDb.query(
            tableName,
            null,
            con.getWhereClauseSafe(),
            con.getWhereArgsSafe(),
            null,
            null,
            order,
            limitStr
        )

        //如果游标不为空，则解析填充好List返回
        query?.apply {
            val resultList = dbClassParser.makeResultList(entity, this, columnFieldMap)
            query.close()//切记关闭游标！！游标实际上就是打开了一条数据库文件读取的io流！
            return resultList
        }
        return null
    }

    /**
     * 条件封装类，把update的条件封装到里面方便操作
     * Map : _id -> 1, name -> andy, ....
     */
    private class Condition(whereCause: Map<String, String?>) {
        private lateinit var whereClause: String//_id = ? and name = ? and ...
        private lateinit var whereArgs: Array<String>//[1, andy, ...]

        init {
            val conList: ArrayList<String> = arrayListOf()

            val conditionString = buildString {
                //遍历传入的Map，拼接条件语句
                // _id = ? and name = ? and ...
                for (entry in whereCause) {
                    entry.value?.let { value ->
                        append("${entry.key} =? and ")
                        conList.add(value)//[1, andy, ...]
                    }
                }
                if (this.length > 6)
                    delete(this.length - 5, this.length - 1)//删掉末尾的and和空格
            }
            if (conList.size > 0)
                whereArgs = conList.toTypedArray()
            if (conditionString.isNotEmpty())
                whereClause = conditionString
        }

        fun getWhereClauseSafe(): String {
            return if (!::whereClause.isInitialized) "" else whereClause
        }

        fun getWhereArgsSafe(): Array<String>? {
            return if (!::whereArgs.isInitialized) null else whereArgs
        }
    }
}