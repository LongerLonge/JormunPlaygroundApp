package com.jormun.likeroom.base

//对外提供的BaseDao接口。
interface IBaseDao<T : Any> {

    suspend fun insert(entity: T?): Long//插入

    suspend fun update(entity: T?, where: T?): Int//指定插入

    suspend fun delete(entity: T?): Int//删除

    suspend fun query(entity: T?): List<T>?//查询

    suspend fun query(entity: T?, order: String?, startIndex: Int?, limit: Int?): List<T>?//按条件查询

}