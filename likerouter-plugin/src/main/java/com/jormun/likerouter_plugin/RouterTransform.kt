package com.jormun.likerouter_plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.jar.JarFile

class RouterTransform(val project: Project) : Transform() {
    private lateinit var routerClassJarFile: File
    private val registerClassList = mutableListOf<String>()

    //给这个tf命个名
    override fun getName(): String {
        return "router-register"
    }

    //要处理什么类型的输入，这里我们要插桩所以需要处理的是Class类型的输入。
    //在javac编译好了class文件后gradle会直接丢给这些指定了处理Class的TF类进行下一步处理。
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

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        //因为我们需要对class文件进行插桩，所以我们需要做：
        //1.把原来的class文件拷贝一份到我们的处理目录
        //2.因为class文件里面可能有继承Android系统library的类(比如AppActivity)，所以需要把Jar包也复制过去。
        //可能有人疑惑为什么要复制Jar包和Class文件，因为ASM会在我们插桩代码的时候自动帮我们导包，所以我们必须得保证该处理环境所有东西齐全！

        transformInvocation.apply {
            //清空一下输出端路径里面所有文件，因为我们不是增量编译
            if (!isIncremental) outputProvider.deleteAll()

            //--------------------------------------JAR--------------------------------------------
            //****app的所有依赖最终都会被转化成Jar包，包括依赖的其它Module
            //先复制Jar包过去，确保不会缺东西
            for (input in inputs) {
                println("start process jar!!")
                for (jarInput in input.jarInputs) {
                    val jarOutputDir = outputProvider.getContentLocation(
                        jarInput.name,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        Format.JAR
                    )
                    //因为不一定是文件夹，有可能是文件，需要判断
                    if (jarInput.file.isDirectory) {
                        FileUtils.copyDirectory(jarInput.file, jarOutputDir)
                    } else if (jarInput.file.isFile) {
                        FileUtils.copyFile(jarInput.file, jarOutputDir)
                        scanJar(jarInput.file, jarOutputDir)
                    }

                }

                //------------------------------------CLASS----------------------------------------
                //****app本身的kt或者java文件才会被编译进这个class文件夹列表，不包括依赖的东西。
                //然后需要获得class文件的路径，作为输入端，复制去我们的目录
                for (directoryInput in input.directoryInputs) {
                    val classInputDirName = directoryInput.name
                    //这个就是Gradle编译时class文件的路径，也就是javac编译后的class文件所在地
                    //val absolutePath = directoryInput.file.absolutePath

                    //使用输入端的信息生成输出端
                    val classOutputDir = outputProvider.getContentLocation(
                        classInputDirName,
                        directoryInput.contentTypes,
                        directoryInput.scopes,
                        Format.DIRECTORY
                    )
                    //找到目标类(IRouter的子类)
                    findTarget(directoryInput.file, directoryInput.file.absolutePath)
                    //把输入端的东西(Class文件)拷贝到输出端
                    if (directoryInput.file.isDirectory) {
                        FileUtils.copyDirectory(directoryInput.file, classOutputDir)
                    } else if (directoryInput.file.isFile) {
                        FileUtils.copyFile(directoryInput.file, classOutputDir)
                    }
                }
            }
        }
        //遍历了所有Class和Jar文件之后假如发现有目标的IRouter实现类就执行字节码插入操作
        if (::routerClassJarFile.isInitialized) {
            RegisterCodeGenerator.insertInitCodeToRouterRegisterClass(
                registerClassList,
                routerClassJarFile
            )
        }
    }

    //在一堆class文件中找到目标类(IRouter的子类)
    private fun findTarget(inputClazzFile: File?, inputClassFilePath: String) {
        if (inputClazzFile == null) {
            println("find target err: file not null!")
            return
        }
        if (inputClazzFile.isDirectory) {
            //如果是文件夹则递归查询
            val listFiles = inputClazzFile.listFiles()
            if (listFiles == null) {
                println("find target err: listFiles not null!")
                return
            }
            for (listFile in listFiles) {
                findTarget(listFile, inputClassFilePath)
            }
        } else {
            //是文件的话就直接查询是否IRouter的子类
            val clazzFilePath = inputClazzFile.absolutePath
            if (!clazzFilePath.endsWith(".class")) return

            if (clazzFilePath.contains("R$") || clazzFilePath.contains("R.class") ||
                clazzFilePath.contains("BuildConfig.class")
            ) return
            println("finTarget path clazzFilePath: $clazzFilePath")
            //因为win默认是com\your\haha\xxxx的结构，所以要替换成java识别的
            val path = clazzFilePath.replace("\\", "/")
            println("finTarget path after replace: $path")
            if (shouldProcessClass(path)) {
                try {
                    println("finTarget path start scanClass: $path")
                    scanClass(FileInputStream(path))
                } catch (e: Exception) {
                    println("finTarget path err: ")
                    e.printStackTrace()
                }
            }
            /* if (clazzFilePath.startsWith("RouterInject")) {
                 //clazzFilePath.
             }

             if (clazzFilePath.contains("Application")) {

             }*/
        }
    }

    //扫描Jar包
    private fun scanJar(src: File, dest: File) {
        val jarFile = JarFile(src)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val jarElement = entries.nextElement()
            val jarEntryName = jarElement.name
            //找到KSP生成的类：
            if (shouldProcessClass(jarEntryName)) {
                //project.logger.log(LogLevel.DEBUG, "find class success: $name")
                println("find class success: $jarEntryName")
                val inputStream = jarFile.getInputStream(jarElement)
                scanClass(inputStream)
            } else if (jarEntryName == "${ConstValue.ROUTER_PREFIX}/MyRouter.class") {//找到Router类

                println("find router class success: $jarEntryName")
                routerClassJarFile = dest
                break
            }
        }
    }

    //扫描出IRouter的实现类，但凡是这个实现类都代表是需要进行注册操作的类。
    //上面扫描jar包过滤出类之后，还要进行这步的操作是因为怕有重名或者其它情况，这里是进一步容错和精确目标类。
    private fun scanClass(inputStream: InputStream?) {
        if (inputStream == null) {
            println("scanClass err, inputStream is null!")
            return
        }
        //通过ClassVisitor去查询是否IRouter的子类
        val classReader = ClassReader(inputStream)
        val scanClassVisitor = ScanClassVisitor(Opcodes.ASM5)
        classReader.accept(scanClassVisitor, ClassReader.EXPAND_FRAMES)
        inputStream.close()
    }

    //判断是否需要进行处理的类
    private fun shouldProcessClass(classFullName: String?): Boolean {
        //这里写死的生成包名为com.your.haha，为了演示。
        //因为编译环境暂时默认为win，所以.要换成/
        return classFullName != null && classFullName.contains("com/your/haha")
                && classFullName.endsWith(".class")
    }

    //进行Class扫描需要通过ClassVisitor
    inner class ScanClassVisitor(api: Int) : ClassVisitor(api) {
        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
            //取出该类所有的实现接口，遍历看下里面有没有我们的目标接口(IRouter)，有就是代表这个类是我们的目标类
            interfaces?.apply {
                for (interfaceName in this) {
                    if (interfaceName == "${ConstValue.ROUTER_PREFIX}/IRouter") {
                        println("scan class, interfaceName is: $name")
                        if (name != null && name.isNotEmpty() && !registerClassList.contains(name))
                            registerClassList.add(name)
                    }
                }
            }
        }
    }
}