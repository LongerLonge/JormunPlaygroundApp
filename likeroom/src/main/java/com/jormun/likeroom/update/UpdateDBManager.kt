package com.jormun.likeroom.update

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.jormun.likeroom.db.BaseSQLDatabaseHelper
import com.jormun.likeroom.utils.FileUtil
import org.w3c.dom.Document
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStream
import java.util.ArrayList
import javax.xml.parsers.DocumentBuilderFactory

class UpdateDBManager(private val context: Context) {

    companion object {
        const val UPDATE_TAG = "UPDATE_TAG"
        const val INFO_FILE_DIV = "/"
    }


    private val parentFile = File(context.dataDir, "update")
    private val dbParentDirPath = "${context.dataDir.absolutePath}/databases"
    private var currentDBVersion = ""//当前APP本地数据的版本
    private var lastBackupVersion = ""//当前APP本地数据库最新的备份版本信息(通常为上一个版本的备份)
    private lateinit var sqlDatabaseHelper: BaseSQLDatabaseHelper


    suspend fun doDataBaseUpdate(
        createNewTableSql: String,
        updateXmlName: String,
        sqlDatabaseHelper: BaseSQLDatabaseHelper
    ): Boolean {
        this.sqlDatabaseHelper = sqlDatabaseHelper
        //1. 解析xml文件成为封装对象
        val readDbXml = readDbXml(context, updateXmlName)
        if (readDbXml == null) {
            Log.e(UPDATE_TAG, "doDataBaseUpdate: read update xml fail!")
            return false
        }
        //2.获取本地版本信息
        //getLocalVersionInfo()
        if (true) {
            //3.获取更新的表信息
            //val updateNode = parseAndGetUpdateDbNode(readDbXml, lastBackupVersion, currentDBVersion)
            val updateNode = parseAndGetUpdateDbNode(readDbXml, "V001", "V002")
            if (updateNode == null) {
                Log.e(UPDATE_TAG, "doDataBaseUpdate err: get update node fail！")
                return false
            }

            val updateNodeList = updateNode.updateNodeList//取出更新表的Node节点，包含sql语句
            if (updateNodeList.isEmpty()) {
                Log.e(UPDATE_TAG, "doDataBaseUpdate err: get updateNodeList fail！")
                return false
            }

            val versionTo = updateNode.versionTo//取出需要升级到的数据库版本
            if (versionTo.isEmpty()) {
                Log.e(UPDATE_TAG, "doDataBaseUpdate err: get version to fail！")
                return false
            }

            //4. 解析出创建table的语句和name等
            val createVersionNode = parseAndGetCreateVersionNode(readDbXml, versionTo)

            //5.备份当前涉及到更新的数据库
            for (updateDbNode in updateNodeList) {
                val oldPath = "${dbParentDirPath}/${updateDbNode.name}"
                val newPath = "${dbParentDirPath}/${updateDbNode.name}_bk"
                sqlDatabaseHelper.closeAllLink()
                FileUtil.copySingleFile(oldPath, newPath)
            }

            //6.开始执行建表建库前的sql语句：
            executeUpdateNodeDbSQL(updateNodeList, -1)


            //7.开始执行建表建库的sql语句
            executeCreateVersion(createVersionNode)
            //executeCreateVersion(createNewTableSql)
            // executeSQL(getSqliteDataBase(updateNodeList[0].name), arrayListOf(createNewTableSql))
            Log.e(UPDATE_TAG, "doDataBaseUpdate : create db success")


            //8.开始执行建表建库后的sql语句
            executeUpdateNodeDbSQL(updateNodeList, 1)

            //9.执行成功后写入文本文档进行保存
            saveVersionInfo(versionTo, currentDBVersion)

            //10.更新所有操作成功：
            Log.e(UPDATE_TAG, "doDataBaseUpdate: all success!")
        }

        return true
    }


    /**
     * 执行更新数据的SQL语句
     * @param updateDbNodes : xml中定义好的updateNode节点
     * @param type : 类型，-1是建表前(before)，1是建表后(after)
     */
    private suspend fun executeUpdateNodeDbSQL(updateDbNodes: List<UpdateDbNode?>, type: Int) {
        for (updateDbNode in updateDbNodes) {
            if (updateDbNode == null || updateDbNode.name.isEmpty()) {
                Log.e(UPDATE_TAG, "executeDbSQL err: updatedb node is null!")
                return
            }
            val sqlStringList = when {
                type < 0 -> {
                    Log.e(UPDATE_TAG, "executeDbSQL: type is -1!")
                    updateDbNode.sqlBeforeList
                }
                type > 0 -> {
                    Log.e(UPDATE_TAG, "executeDbSQL: type is 1!")
                    updateDbNode.sqlAfterList
                }
                else -> {
                    Log.e(UPDATE_TAG, "executeDbSQL err: type is 1 or -1!")
                    null
                }
            }
            if (sqlStringList == null || sqlStringList.isEmpty()) {
                Log.e(UPDATE_TAG, "executeDbSQL err: sqlStringList is null!")
                return
            }
            if (updateDbNode.name.isEmpty()) {
                Log.e(UPDATE_TAG, "executeDbSQL err: db name is empty!")
                return
            }
            val sqliteDataBase = getSqliteDataBase(updateDbNode.name)

            if (sqliteDataBase == null) {
                Log.e(UPDATE_TAG, "executeDbSQL err: get db is null!")
                return
            }
            //if (sqliteDataBase)
            //全部检查完毕，执行sql事务
            executeSQL(sqliteDataBase, sqlStringList)
            sqliteDataBase.close()
            Log.e(UPDATE_TAG, "executeDbSQL type=$type success!")
        }
    }

    /**
     * 开始执行创建数据库的操作
     * 对应的是xml中createDb这个节点
     */
    private fun executeCreateVersion(createVersionNode: CreateVersionNode?) {
        if (createVersionNode == null) {
            Log.e(UPDATE_TAG, "create db err: createVersion is null!")
        } else {
            val createDBList = createVersionNode.createDBList
            if (createDBList.isNotEmpty()) {
                for (createDBNode in createDBList) {
                    val name = createDBNode.name//取出表名字
                    val createSqlList = createDBNode.sqlStringList//取出创建的sql语句，有可能为多句
                    if (name.isNotEmpty() && createSqlList.isNotEmpty()) {
                        //获取sql对象并且创建db
                        val sqLiteDatabase = getSqliteDataBase(name)
                        //然后执行里面的建表语句
                        executeSQL(sqLiteDatabase, createSqlList)
                    } else {
                        Log.e(UPDATE_TAG, "create db err: db name or create sql is null!!")
                    }
                }
            }
        }
    }

    /**
     * 根据给定的数据执行sql语句
     * @param sqLiteDatabase : 数据库
     * @param sqlList : 需要执行的sql语句
     */
    private fun executeSQL(sqLiteDatabase: SQLiteDatabase?, sqlList: ArrayList<String>) {
        Log.e(UPDATE_TAG, "executeSQL: db isOpen:${sqLiteDatabase?.isOpen}")
        if (sqLiteDatabase == null || sqlList.isEmpty() || !sqLiteDatabase.isOpen) {
            Log.e(UPDATE_TAG, "executeSQL err: sqlDatabase null or sqlList null!")
        } else {
            //开启事务，事务的意思就是要执行到最后都成功才算成功，其中一个不成功则整体失败取消回退。
            sqLiteDatabase.beginTransaction()
            try {
                for (rawSqlStr in sqlList) {
                    val tempStr = rawSqlStr.replace("\r\n", "")
                    val finalSql = tempStr.replace("\n", "")
                    if (rawSqlStr.trim() != "") {
                        sqLiteDatabase.execSQL(finalSql)
                    }
                }
                sqLiteDatabase.setTransactionSuccessful()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                sqLiteDatabase.endTransaction()
            }
        }
    }

    /**
     * 根据给定的名字创建数据库
     */
    private fun getSqliteDataBase(name: String): SQLiteDatabase? {
        try {
            if (::sqlDatabaseHelper.isInitialized) {
                return sqlDatabaseHelper.openWriteDbLink()
            } else {
                val dbPath = "${dbParentDirPath}/${name}"
                return SQLiteDatabase.openOrCreateDatabase(dbPath, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }


    /**
     * @param updateDbXml :解析好的dbxml封装对象，里面包含所有节点的数据
     * @param targetVersion : 升级到目标数据库的版本
     * 根据目标数据库版本，从xml封装对象中提取出create信息，并且返回封装好的createVersion对象
     */
    private fun parseAndGetCreateVersionNode(
        updateDbXml: UpdateDbXmlNode?, targetVersion: String?
    ): CreateVersionNode? {
        var createVersion: CreateVersionNode? = null
        if (updateDbXml == null || targetVersion == null || targetVersion.isEmpty()) {
            return createVersion
        }
        val createVerNodeList = updateDbXml.createVerNodeList
        if (createVerNodeList.isNotEmpty()) {
            for (createVersionNode in createVerNodeList) {
                val split = createVersionNode.versionName.trim().split(",")//有可能是V001,V002,V003这种
                for (s in split) {
                    if (s.trim() == targetVersion) {
                        createVersion = createVersionNode
                        break
                    }
                }
            }
        }
        return createVersion
    }

    /**
     * 解析出升级数据库的节点并且返回
     * @param updateDbXml : 更新Document，也就是解析后的xml封装类
     * @param lastVersion : APP备份的上一个版本
     * @param currentVersion : APP本地当前版本
     */
    private fun parseAndGetUpdateDbNode(
        updateDbXml: UpdateDbXmlNode?,
        lastVersion: String,
        currentVersion: String
    ): UpdateStepNode? {
        var updateStepNode: UpdateStepNode? = null
        if (updateDbXml == null) {
            Log.e(UPDATE_TAG, "parseAndGetUpdateDbNode: updateDbXml is null!")
        } else {
            val updateStepNodeList = updateDbXml.updateStepNodeList
            if (updateStepNodeList.isNotEmpty()) {
                for (updateNode in updateStepNodeList) {
                    val versionFrom = updateNode.versionFrom
                    val versionTo = updateNode.versionTo
                    if (versionFrom == null || versionTo == null) {
                        Log.e(UPDATE_TAG, "parseAndGetUpdateDbNode: updateDbXml is null!")
                    } else {
                        //分解VersionFrom，因为可能有跨版本，比如：V001,V002,V003
                        //就意味着从01升级到03，需要切割一下","来判断是否包含当前需要升级的版本
                        val versionSplit = versionFrom.split(",")
                        if (versionSplit.isNotEmpty()) {
                            if (versionSplit.contains(currentVersion)) {//只要是包含在VersionFrom里面的，都是需要升级的
                                updateStepNode = updateNode
                                break
                            }
                        }
                    }
                }
            }
        }
        return updateStepNode
    }


    /**
     * 读取并且解析升级的xml文件
     * 封装成我们定义的对象返回方便使用！
     */
    private fun readDbXml(context: Context, updateXmlName: String): UpdateDbXmlNode? {
        var input: InputStream? = null
        var document: Document? = null
        try {
            //input = context.assets.open("updateXml.xml")
            input = context.assets.open(updateXmlName)
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            document = docBuilder.parse(input)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            input?.close()
        }
        if (document == null) {
            Log.e("TAG", "readDbXml: read an parse xml document fail!")
            return null
        }
        return UpdateDbXmlNode(document)
    }

    /**
     * 更新数据库后，把信息写入到本地txt文件中进行保存
     * @param newVersion : 新版本信息
     */
    private fun saveVersionInfo(newVersion: String, versionFrom: String): Boolean {
        var fileWriter: FileWriter? = null
        try {
            val file = File(parentFile, "update.txt")
            if (!parentFile.exists()) parentFile.mkdir()
            if (!file.exists()) file.createNewFile()
            fileWriter = FileWriter(file, false)
            fileWriter.write("${newVersion}${INFO_FILE_DIV}${versionFrom}")//暂时写死V002模拟当前版本是002
            fileWriter.flush()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fileWriter?.close()
        }
        return false
    }

    /**
     * 读取本地版本文件，提取出APP本地数据库的当前版本和备份版本。
     * currentDBVersion //APP当前本地数据库版本
     * lastBackupVersion //APP当前备份的数据库版本
     */
    private fun getLocalVersionInfo(): Boolean {
        var isSuccess = false
        val versionFile = File(parentFile, "update.txt")
        if (parentFile.exists() && versionFile.exists()) {
            var byteRead = 0
            val tempByte = ByteArray(100)
            var input: InputStream? = null
            try {
                input = FileInputStream(versionFile)
                val versionString = buildString {
                    while (byteRead != -1) {
                        append(String(tempByte, 0, byteRead))
                        byteRead = input!!.read(tempByte)
                    }
                }
                val infoStrings = versionString.split(INFO_FILE_DIV)
                if (infoStrings.size >= 2) {
                    currentDBVersion = infoStrings[0]//当前APP本地数据的版本
                    lastBackupVersion = infoStrings[1]//当前APP本地数据库最新的备份版本信息
                    isSuccess = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(UPDATE_TAG, "getLocalVersionInfo err:")
            } finally {
                input?.close()
                input = null
            }
        }
        return isSuccess
    }

}