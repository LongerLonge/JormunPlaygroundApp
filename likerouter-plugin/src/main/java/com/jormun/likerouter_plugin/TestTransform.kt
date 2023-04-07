package com.jormun.likerouter_plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Project

class TestTransform (val project: Project) : Transform() {
    //返回该tf的名字
    override fun getName(): String {
        return "test_transform"
    }

    //要处理什么类型的输入，这里我们要插桩所以需要处理的是Class类型的输入。
    //javac 在编译好了class文件后会直接丢给这些指定了处理Class的TF类进行下一步处理
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    //作用域也就是范围，我们选的是整个工程。
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //是否开启增量编译
    override fun isIncremental(): Boolean {
        return false
    }

    //这里就是真正的处理方法，在里面写我们要写的代码！
    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        transformInvocation.apply {
            //遍历输入的文件
            for (input in inputs) {
                //这个代表的是Jar文件(外部引用或者依赖的都会打包成Jar)
                for (jarInput in input.jarInputs) {
                    break
                }
                //这个代表的是Class文件(属于项目自己本身的代码文件会被编译成Class丢进这里)
                for (directoryInput in input.directoryInputs) {
                    break
                }
            }
        }
    }
}