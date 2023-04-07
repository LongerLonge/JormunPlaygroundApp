package com.jormun.likeroom.update

import org.w3c.dom.Element

class CreateVersionNode(element: Element) {

    var versionName: String

    val createDBList = arrayListOf<CreateDBNode>()


    init {
        versionName = element.getAttribute("version")
        val elementsByTagName = element.getElementsByTagName("createDb")
        elementsByTagName?.let { nodeList ->
            val length = nodeList.length
            if (length > 0) {
                for (i in 0 until length) {
                    val elementItem = nodeList.item(i) as Element
                    val cbNode = CreateDBNode(elementItem)
                    createDBList.add(cbNode)
                }
            }
        }
    }

}