package com.jormun.likerouter

import android.app.Activity

/**
 * 定义一个顶层接口，让子类实现并且把当前Module里面所有注解了route的对象塞进Router里面
 */
interface IRouter {
    fun putActivity(routesMap: MutableMap<String, Class<out Activity>>)
}