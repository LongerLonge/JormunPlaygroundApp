package com.jormun.likeroom.update

import org.w3c.dom.Element

class UpdateStepNode(element: Element) {

    var versionFrom: String
    var versionTo: String

    val updateNodeList = arrayListOf<UpdateDbNode>()

    init {
        versionFrom = element.getAttribute("versionFrom")?:""
        versionTo = element.getAttribute("versionTo")?:""

        val rawUpdateNodeList = element.getElementsByTagName("updateDb")
        val length = rawUpdateNodeList?.length
        length?.let {
            for (i in 0 until length) {
                val updbEle = rawUpdateNodeList.item(i) as Element
                val updateDbNode = UpdateDbNode(updbEle)
                updateNodeList.add(updateDbNode)
            }
        }
    }
}