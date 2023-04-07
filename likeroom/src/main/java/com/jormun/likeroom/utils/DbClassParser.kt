package com.jormun.likeroom.utils

import android.database.Cursor
import android.util.Log
import com.jormun.likeroom.an.DbField
import com.jormun.likeroom.an.DbMainKey
import com.jormun.likeroom.an.DbTable
import com.jormun.likeroom.base.IClassParser
import com.jormun.likeroom.utils.TAGS.CLASSPARSERTAG
import java.lang.reflect.Field

/**
 * 帮忙把Class解析成Map，并且把数据库返回的数据封装成对应的类返回
 */
class DbClassParser : IClassParser {


    override fun getTableNameFromEntity(entity: Class<*>): String {
        //通过注解自动获得表名，要是没有注解就直接获得类名作为表名
        return entity.getAnnotation(DbTable::class.java)?.dbname ?: run {
            entity.simpleName
        }
    }

    override fun getMainKeyFromEntity(entity: Class<*>): String {
        var mainKeyName = ""
        try {
            val declaredFields = entity.declaredFields
            for (field in declaredFields) {
                field.isAccessible = true
                field.getAnnotation(DbMainKey::class.java)?.apply {
                    mainKeyName = field.getAnnotation(DbField::class.java)?.fieldName ?: run {
                        field.name
                    }
                }
                break
            }
            return mainKeyName

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(CLASSPARSERTAG, "getMainKeyFromEntity err: ")
        }
        return mainKeyName
    }

    /**
     * 解析并且初始化列名和Field的Map
     * Key(列名) -> Value(Field)
     * 这样就可以很方便的取出Field并且调用
     */
    override fun parseColumFieldMap(
        entity: Class<*>,
        columnNames: Array<String>
    ): Map<String, Field>? {
        var fieldMap: Map<String, Field>? = null
        try {
            val declaredFields = entity.declaredFields
            for (f in declaredFields) {
                f.isAccessible = true
            }
            fieldMap = mutableMapOf()
            for (columnName in columnNames) {//遍历表里面的列名(字段名)

                var columField: Field? = null

                for (field in declaredFields) {//取出对象里面的成员变量名(字段名)
                    //加了注解的直接拿出里面的filedName，没有的直接拿出里面的filedName
                    val filedName = field.getAnnotation(DbField::class.java)?.fieldName ?: run {
                        field.name
                    }
                    if (columnName == filedName) {//假如一致则说明字段一致，需要保存Field方便后面使用
                        columField = field
                        break
                    }
                }
                // _id -> Filed(1001)
                // name -> Filed("jack")
                // password -> Filed("1111")
                columField?.apply {
                    fieldMap[columnName] = this
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(CLASSPARSERTAG, "parseColumFieldMap: err")
        }
        return fieldMap
    }

    override fun <T : Any> getFieldValueMap(
        entity: T?,
        fieldList: List<Field>
    ): Map<String, String> {
        val resultMap = mutableMapOf<String, String>()
        if (entity == null) {
            Log.e(CLASSPARSERTAG, "getKeyValuesMap err: entity is not null!")
            return resultMap
        }
        for (field in fieldList) {
            field.isAccessible = true
            field.get(entity)?.apply {
                val key = field.getAnnotation(DbField::class.java)?.fieldName ?: run {
                    field.name
                }
                resultMap[key] = toString()
            }
        }
        return resultMap //([_id->1001],[name->jack]...)
    }

    override fun <T : Any> makeMainKeyEntity(entity: T, mainKeyField: Field?): T? {
        if (mainKeyField == null) {
            Log.e(CLASSPARSERTAG, "makeMainKeyEntity: main_key is not null!")
            return null
        }
        //user("1","jack","111")
        //user("1")
        val mainKeyValue = mainKeyField.get(entity)
        val mainKeyEntity = entity::class.java.newInstance()
        mainKeyField.set(mainKeyEntity, mainKeyValue)
        return mainKeyEntity
    }

    override fun <T : Any> makeResultList(
        entity: T,
        query: Cursor,
        columnFieldMap: Map<String, Field>
    ): List<T> {
        val listEntity = arrayListOf<T>()
        var item: Any?
        while (query.moveToNext()) {//如果游标不为空且有下一位的话
            item = entity::class.java.newInstance()//初始化相应的实例
            for (kv in columnFieldMap) {//遍历cacheMap [_id -> Field(id), name -> Field(name), ...]
                //把成员变量，实际上也就是字段名去查出该字段在表中是第几列
                // 因为id在第一列，因此用id字段名去查的话，返回的index就是=1(或者0)
                // 又因为query.next出来的是该表的每一行，所以下面这个代码实际上就是取出 "第一行"的"第一列"数据
                // 如下所示：
                //        |table_user|
                //        |id|name|password|
                // query->|1|andy|333|
                //         ^
                //     columIndex
                val columName = kv.key
                val columnIndex = query.getColumnIndex(columName)
                //获取字段类型type
                val type = kv.value.type
                if (columnIndex != -1) {//如果该表中有这一列的话
                    when (type) {//判断类型，不同类型封装到不同的成员变量里,同时用反射的set方法把值塞到实例里面
                        Integer::class.java -> kv.value.set(item, query.getInt(columnIndex))
                        String::class.java -> kv.value.set(item, query.getString(columnIndex))
                        Long::class.java -> kv.value.set(item, query.getLong(columnIndex))
                        Double::class.java -> kv.value.set(item, query.getDouble(columnIndex))
                        else -> continue
                    }
                }
            }
            //封装好了实例之后，加入到列表中去。
            listEntity.add(item)
        }
        return listEntity
    }

}