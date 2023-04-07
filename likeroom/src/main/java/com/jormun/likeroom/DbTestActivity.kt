package com.jormun.likeroom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.jormun.likerouter.MyRouter
import com.jormun.likerouter_annotation.Route

@Route("db/test")
class DbTestActivity : AppCompatActivity() {

    private lateinit var dbTestViewModel: DbTestViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_db_test)
        dbTestViewModel = defaultViewModelProviderFactory.create(DbTestViewModel::class.java)
    }

    fun createDB(view: View) {
        dbTestViewModel.createDB(this)
    }

    fun insertDataToDB(view: View) {
        dbTestViewModel.insertDataToDB(this)
    }

    fun updateToDB(view: View) {

        dbTestViewModel.updateToDB(this)
    }

    fun deleteDataFromDB(view: View) {
        dbTestViewModel.deleteDataFromDB(this)
    }

    fun queryFromDB(view: View?) {

        val uLd = dbTestViewModel.queryFromDB(this)
        uLd.observe(this) {
            if (it != null && it.isNotEmpty()) {
                Toast.makeText(
                    this@DbTestActivity,
                    "queryFromDB: ${it.size}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getExtensionDao(view: View) {
//        val extensionDao =
//            BaseDaoFactory.getExtensionDao(PersonDaoImpl::class.java, Person::class.java)
    }

    fun doSeparateOperation(view: View) {
        dbTestViewModel.doSeparateOperation(this)
    }

    fun doCheckExitsInsert(view: View) {
        dbTestViewModel.doCheckExitsInsert(this)
    }

    fun doUpdateDbToCreate(view: View) {
        //val baseSQLDBHelper = BaseSQLDbHelperFactory.getBaseSQLDBHelper<User>(this, "user")
        //baseSQLDBHelper.closeAllLink()
        //注意升级前，一定要close所有db！！！！！

    }

    fun returnMain(view: View) {
        //room库并没有依赖app module，能跳转成功就代表Router能正常工作
        MyRouter.sInstance.jumpActivity(this,"main/main",null)
        finish()
    }
}