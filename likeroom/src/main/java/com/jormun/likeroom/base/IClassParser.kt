package com.jormun.likeroom.base

import android.database.Cursor
import java.lang.reflect.Field

interface IClassParser {
    /**
     * 解析对象，返回表名
     * @param entity : 需要解析的对象
     */
    fun getTableNameFromEntity(entity: Class<*>): String


    /**
     * 解析对象，返回主键名
     * @param entity : 需要解析的对象
     */
    fun getMainKeyFromEntity(entity: Class<*>): String

    /**
     * 解析对象，通过列名跟成员变量对应
     * @param entity : 需要解析的对象
     * @param columnNames : 查询出该表的所有列名
     * @return 封装好的Map，key是列名，Field类中对应的成员变量
     */
    fun parseColumFieldMap(entity: Class<*>, columnNames: Array<String>): Map<String, Field>?

    /**
     * 解析对象，把字段和实体对象里面的值映射到一起。
     * @param entity : 需要解析的对象
     * @param fieldList : 字段List，用来反射取出里面的值
     * @return :字段和具体值映射好的Map
     */
    fun <T : Any> getFieldValueMap(entity: T?, fieldList: List<Field>): Map<String, String>

    /**
     * 创建一个主键实体对象返回给调用者
     * @param entity : 需要解析的实体对象
     * @param mainKeyField : 主键名字
     * @return : 封装好的实体对象
     */
    fun <T : Any> makeMainKeyEntity(entity: T, mainKeyField: Field?): T?

    /**
     * 把游标数据进行再组合，然后塞到对象列表返回
     *@param entity : 需要被封装的类
     *@param query : 查询数据库出来的游标
     *@param columnFieldMap : 解析好的列和字段Map，见 parseColumFieldMap
     *@return : 封装好的对象列表
     */
    fun <T : Any> makeResultList(
        entity: T,
        query: Cursor,
        columnFieldMap: Map<String, Field>
    ): List<T>
}