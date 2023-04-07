package com.jormun.likeroom.update

import org.w3c.dom.Element

class CreateDBNode(element: Element) {
    var name: String
    val sqlStringList = arrayListOf<String>()

    init {
        name = element.getAttribute("name")
        val sqlCreateNodesList = element.getElementsByTagName("sql_createTable")
        sqlCreateNodesList?.let { nodeList ->
            val length = nodeList.length
            if (length > 0) {
                for (i in 0 until length) {
                    val item = nodeList.item(i)
                    val nodeValue = item.textContent
                    sqlStringList.add(nodeValue)
                }
            }
        }
    }

}