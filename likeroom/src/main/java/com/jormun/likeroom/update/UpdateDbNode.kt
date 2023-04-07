package com.jormun.likeroom.update

import org.w3c.dom.Element

class UpdateDbNode(element: Element) {

    var name: String

    val sqlBeforeList = arrayListOf<String>()
    val sqlAfterList = arrayListOf<String>()

    init {
        name = element.getAttribute("name") ?: ""
        val beforeList = element.getElementsByTagName("sql_before")
        val afterList = element.getElementsByTagName("sql_after")

        val beforeLength = beforeList?.length
        val afterLength = afterList?.length
        beforeLength?.let {
            for (i in 0 until beforeLength) {
                val textContent = beforeList.item(i).textContent
                sqlBeforeList.add(textContent)
            }
        }
        afterLength?.let {
            for (i in 0 until afterLength) {
                val textContent = afterList.item(i).textContent
                sqlAfterList.add(textContent)
            }
        }
    }

}