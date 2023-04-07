package com.jormun.likeroom.update

import org.w3c.dom.Document
import org.w3c.dom.Element

class UpdateDbXmlNode(document: Document) {

    val createVerNodeList = arrayListOf<CreateVersionNode>()
    val updateStepNodeList = arrayListOf<UpdateStepNode>()


    init {

        val createVersionElements = document.getElementsByTagName("createVersion")
        val updateStepElements = document.getElementsByTagName("updateStep")

        val createElementsLength = createVersionElements?.length
        val updateStepElementsLength = updateStepElements?.length

        createElementsLength?.let {
            for (i in 0 until createElementsLength) {
                val createElement = createVersionElements.item(i) as Element
                val createVersionNode = CreateVersionNode(createElement)
                createVerNodeList.add(createVersionNode)
            }
        }

        updateStepElementsLength?.let {
            for (i in 0 until updateStepElementsLength) {
                val updateElement = updateStepElements.item(i) as Element
                val updateStepNode = UpdateStepNode(updateElement)
                updateStepNodeList.add(updateStepNode)
            }
        }
    }

}