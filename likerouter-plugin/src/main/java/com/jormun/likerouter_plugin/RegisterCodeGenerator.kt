package com.jormun.likerouter_plugin

import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

object RegisterCodeGenerator {

    //对Jar包里面的IRouter子类进行注册
    fun insertInitCodeToRouterRegisterClass(registerList: List<String>, routerClassJarFile: File?) {
        if (registerList.isEmpty() || routerClassJarFile == null) {
            println("insertInitCodeToRouterRegisterClass err, registerList null or jarFile null!")
            return
        }
        var jarOutputStream: JarOutputStream? = null
        var jarInputStream: InputStream? = null
        var jarFile: JarFile? = null
        println("RegisterCodeGenerator.insertInitCodeToRouterRegisterClass start!!")
        try {
            //在源文件(也就是MyRouter.class)下面创建一个.opt的临时文件(MyRouter.opt)
            val optJar = File(routerClassJarFile.parent, "${routerClassJarFile.name}.opt")
            if (optJar.exists()) optJar.delete()
            //进行Jar包的遍历，大部分都是模板代码了
            jarFile = JarFile(routerClassJarFile)
            val entries = jarFile.entries()
            jarOutputStream = JarOutputStream(FileOutputStream(optJar))
            while (entries.hasMoreElements()) {
                val jarElement = entries.nextElement()
                val jarElementName = jarElement.name
                val zipEntry = ZipEntry(jarElementName)
                jarInputStream = jarFile.getInputStream(jarElement)
                jarOutputStream.putNextEntry(zipEntry)

                if (jarElementName == "${ConstValue.ROUTER_PREFIX}/MyRouter.class") {
                    //找到目标类，进行Class字节码改写
                    val hackedBytes = referHackWhenInit(registerList, jarInputStream)
                    hackedBytes?.apply {
                        jarOutputStream.write(this)
                    }
                } else {//如果不是，则直接写进去不需要修改任何东西
                    jarOutputStream.write(IOUtils.toByteArray(jarInputStream))
                }
                //写完记得关闭stream
                jarInputStream.close()
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            jarFile.close()
            //把源文件删除
            if (routerClassJarFile.exists()) {
                routerClassJarFile.delete()
            }
            //用我们修改后得文件进行替换
            optJar.renameTo(routerClassJarFile)
            println("RegisterCodeGenerator.insertInitCodeToRouterRegisterClass all success!!")
        } catch (e: Exception) {
            println("RegisterCodeGenerator.insertInitCodeToRouterRegisterClass err!!")
            e.printStackTrace()
        } finally {
            //jarOutputStream?.closeEntry()
            //jarOutputStream?.close()
            //jarInputStream?.close()
            //jarFile?.close()
            println("RegisterCodeGenerator.insertInitCodeToRouterRegisterClass finally!!")
        }
    }

    //通过ASM的ClassReader、ClassWriter、ClassVisitor对Class文件的字节码进行改写
    private fun referHackWhenInit(
        registerList: List<String>,
        jarElementInputStream: InputStream
    ): ByteArray? {
        println("RegisterCodeGenerator.referHackWhenInit start!!")
        try {
            val classReader = ClassReader(jarElementInputStream)
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            val myClassVisitor = MyClassVisitor(Opcodes.ASM5, classWriter, registerList)
            classReader.accept(myClassVisitor, ClassReader.EXPAND_FRAMES)
            println("RegisterCodeGenerator.referHackWhenInit all success!!")
            return classWriter.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        println("RegisterCodeGenerator.referHackWhenInit fail: ")
        return null
    }

    //ASM封装的API对象ClassVisitor，用来读取Class文件中字节码的信息
    class MyClassVisitor(api: Int, val classVisitor: ClassVisitor, val registerList: List<String>) :
        ClassVisitor(api, classVisitor) {
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            println("MyClassVisitor.visitMethod start!!")
            var visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
            //找到目标函数，这里并不需要我们手动轮询方法，asm会自动轮询，我们只需要判断即可。
            if (name == "doInit") {
                println("MyClassVisitor.visitMethod doInit start!!")
                //找到之后通过MethodVisitor进行读取并且改写
                visitMethod = RouteMethodVisitor(Opcodes.ASM5, visitMethod, registerList)
            }
            println("MyClassVisitor.visitMethod end!!")
            return visitMethod
        }
    }

    ////ASM封装的API对象MethodVisitor，用来读取Method里面的字节码信息
    class RouteMethodVisitor(
        api: Int,
        private val mv: MethodVisitor,
        private val registerList: List<String>
    ) :
        MethodVisitor(api, mv) {
        override fun visitInsn(opcode: Int) {
            println("RouteMethodVisitor.visitInsn start!!")
            //在函数return之前进行修改
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                for (registerClassName in registerList) {
                    println("RouteMethodVisitor.visitInsn insert code start, name: $registerClassName")
                    //等同于 RouterInjectappapp().putActivity(MyRouter.sInstance.getRouterMap())
                    //mv.visitLabel(Label())
                    mv.visitTypeInsn(Opcodes.NEW, registerClassName)
                    mv.visitInsn(Opcodes.DUP)
                    mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        registerClassName,
                        "<init>",
                        "()V",
                        false
                    )
                    mv.visitFieldInsn(
                        Opcodes.GETSTATIC,
                        "${ConstValue.ROUTER_PREFIX}/MyRouter",
                        "Companion",
                        "L${ConstValue.ROUTER_PREFIX}/MyRouter\$Companion;"
                    )
                    mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "${ConstValue.ROUTER_PREFIX}/MyRouter\$Companion",
                        "getSInstance",
                        "()L${ConstValue.ROUTER_PREFIX}/MyRouter;",
                        false
                    )
                    mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "${ConstValue.ROUTER_PREFIX}/MyRouter",
                        "getRouterMap",
                        "()Ljava/util/Map;",
                        false
                    )
                    mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        registerClassName,
                        "putActivity",
                        "(Ljava/util/Map;)V",
                        false
                    )
                }
            }
            println("RouteMethodVisitor.visitInsn end!!")
            super.visitInsn(opcode)
        }
    }

}