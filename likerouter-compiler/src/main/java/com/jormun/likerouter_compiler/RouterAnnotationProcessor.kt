package com.jormun.likerouter_compiler

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.jormun.likerouter_annotation.Route

/**
 * 注解处理器，给KSP生成代码
 */
//如果通过APT，则需要这个注解接收module的名字，见app/build.gradle -> defaultConfig{...}
//KSP则不用，在app/build.gradle 里面的ksp{...}定义即可，可以去看看
//@SupportedOptions("moduleName")
class RouterAnnotationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("processor start process!!")

        //1.找出被我们目标注解标记的类的符号
        val symbols = resolver.getSymbolsWithAnnotation(Route::class.java.name)
        val iRouterClassDeclaration =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("com.jormun.likerouter.IRouter"))
        if (iRouterClassDeclaration == null) {
            logger.warn("iRouter interface not found!")
            return emptyList()
        }
        //logger.warn("get IRouter success? ${classDeclarationByName?.qualifiedName?.asString()}")

        //2.过滤出我们需要的，因为我们的Route是注解在类上的，所以filter过滤一下KSClassDeclaration(代表该符号是类)
        //第一种写法、通过accept传入Visitor来访问里面的所有元素
        /*symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                it.accept(RouterVisitor(), Unit)
            }*/
        //第二种写法、直接通过filterIsInstance过滤并返回我们想要的符号列表：
        val ksClassSymbols = symbols.filterIsInstance<KSClassDeclaration>()

        //logger.warn("generatedFile size: ${codeGenerator.generatedFile.size}")

        //假如上轮已经处理过了这里就为空，直接返回即可
        if (ksClassSymbols.iterator().hasNext().not()) {
            logger.warn("ksClassSymbolsList is empty.")
            return emptyList()
        }

        //3.开始代码生成：
        generateIRouteCode(ksClassSymbols, iRouterClassDeclaration)

        //4.filter过滤出延迟无效的符号(比如没法解析的)，validate是官方的api过滤用的
        val ret = ksClassSymbols.filter { !it.validate() }.toList()
        logger.warn("this is retFile size: ${ret.size}")

        //这里为什么返回没用的列表，因为KSP支持多轮处理，我们这轮处理了自己感兴趣的符号后，把不需要的延迟无效符号丢给下一轮(假如有的话)
        //当然为了省事你也可以返回个空列表。
        return ret
    }

    //生成Route的收集代码
    private fun generateIRouteCode(
        ksClassSymbols: Sequence<KSClassDeclaration>,
        iRouterClassDeclaration: KSClassDeclaration
    ) {
        //取出我们定义好的Module名，在build.gradle里面透传过来的。
        val moduleName = options["ROUTER_MODULE_NAME"]
        val className = "RouterInject${moduleName}"

        //创建代码文件
        //过滤出当前这个KSClassDeclaration的源文件，只要它不是来源于.class文件的，都会返回它本身的.kt文件。
        //比如WorkActivity.kt是当前这个KSClassDeclaration的源文件，那么就会返回WorkActivity.kt
        val sourceFiles = ksClassSymbols.mapNotNull { it.containingFile }
        sourceFiles.forEach { logger.warn("this is containingFile: ${it.fileName}") }

        val wFile = codeGenerator.createNewFile(
            Dependencies(
                true,
                *sourceFiles.distinct().toList().toTypedArray()
            ),//可以直接用 Dependencies.ALL_FILES，但是性能会差。
            "com.your.haha",//包名，这里我们为了演示先写死包名就是haha
            className //类名，这里是拼接了Module名
        )

        val classText = buildString {
            append("package com.your.haha\n")//这里我们为了演示先写死包名就是haha
            //拼接导包语句
            //因为收集类是继承自IRouter，所以这里要导入它
            //append("import com.jormun.likerouter.IRouter\n")
            append("import ${iRouterClassDeclaration.qualifiedName?.asString()}\n")
            append("import android.app.Activity\n")
            //append("import java.util.Map;\n") kotlin不需要这个
            //因为可能有多个Activity，都需要把它们导进来
            ksClassSymbols.forEach {
                append("import ${it.qualifiedName?.asString()}\n")
            }
            //开始写class文本
            append("class $className : IRouter{\n")
            append("override fun putActivity(routesMap: MutableMap<String, Class<out Activity>>){\n")
            //遍历被注解的类(Activity)并且取出上面注解的参数拼接put语句
            ksClassSymbols.forEach {
                val routeMap = getRouteAnnotationKeyAndValues(it)
                if (routeMap.isNotEmpty())
                    append("routesMap.put(\"${routeMap.keys.first()}\",${routeMap.values.first()})\n")
            }
            append("}\n")
            append("}\n")
        }
        println(classText)
        wFile.write(classText.toByteArray())
        wFile.close()
    }

    //读取出注解里面的value，并且封装成map返回
    private fun getRouteAnnotationKeyAndValues(classDesc: KSClassDeclaration): Map<String, String> {
        val routeMap = mutableMapOf<String, String>()
        //过滤出Route注解, firstOrNull代表只取第一个并且如果列表是空的就返回null
        val annotations =
            classDesc.annotations.filter { it.shortName.asString() == Route::class.java.simpleName }
                .firstOrNull()
        //解析注解上的参数
        annotations?.let {
            it.arguments.forEach { ksValueArgument ->
                val name = ksValueArgument.name?.asString()
                val value = ksValueArgument.value
                when (name) {
                    "path" -> {
                        routeMap[value as String] = "${classDesc.simpleName.asString()}::class.java"
                    }
                    "group" -> {}
                }
            }
        }
        //routeMap = [ "login"->"LoginActivity::class.java", "login/main"->"LoginMainActivity::class.java", ...]
        return routeMap
    }

    //内部类，Visitor，访问符号里面的所有元素
    inner class RouterVisitor : KSVisitorVoid() {
        //这个代表访问“类”元素，因为我们的Route是注解在类上的。
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            val routeMap = mutableMapOf<String, String>()
            val packageName = classDeclaration.containingFile!!.packageName.asString()//包名
            val className = classDeclaration.simpleName.asString()//类名


            //过滤出Route注解, firstOrNull代表只取第一个并且如果列表是空的就返回null
            val annotations =
                classDeclaration.annotations.filter { it.shortName.asString() == Route::class.java.simpleName }
                    .firstOrNull()
            //解析注解上的参数
            annotations?.let {
                it.arguments.forEach { ksValueArgument ->
                    val name = ksValueArgument.name?.asString()
                    val value = ksValueArgument.value
                    when (name) {
                        "path" -> {
                            routeMap[value as String] = "${className}::class.java"
                        }
                        "group" -> {}
                    }
                }
            }

            //如果注解参数列表不为空
            if (routeMap.isNotEmpty()) {

                //取出我们定义好的Module名
                val moduleName = options["ROUTER_MODULE_NAME"]

                //创建代码文件
                val wFile = codeGenerator.createNewFile(
                    Dependencies(false, classDeclaration.containingFile!!),
                    "com.your.haha",//包名
                    "${className}${moduleName}"//类名，这里是拼接了时间戳
                )


                val classText = buildString {
                    append("package com.your.haha")
                    append("\n")
                    append("\n")
                    append("import com.jormun.likerouter.IRouter")
                    append("\n")
                    append("import com.jormun.likerouter.MyRouter")
                    append("\n")
                    append("import ${packageName}.${className}")
                    append("\n")
                    append("class ${className}${moduleName} : IRouter{\n")
                    append("override fun putActivity(){\n")
                    for (pathClass in routeMap) {
                        append("MyRouter.sInstance.addActivity(\"${pathClass.key}\",${pathClass.value})\n")
                    }
                    append("}\n")
                    append("}\n")
                }
                wFile.write(classText.toByteArray())
                wFile.close()
            }
        }
    }

    override fun finish() {
        super.finish()
        logger.warn("processor finish process!")
    }
}