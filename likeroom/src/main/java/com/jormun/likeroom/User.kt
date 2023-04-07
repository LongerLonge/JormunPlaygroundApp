package com.jormun.likeroom

import com.jormun.likeroom.an.DbField
import com.jormun.likeroom.an.DbMainKey
import com.jormun.likeroom.an.DbTable

@DbTable("tb_user")
class User(
    @DbMainKey
    @DbField("_id")
    var id: Int? = null,//Integer   Filed(1001)
    //@DbField("user_name")
    var name: String? = null,//TEXT var char(10)   Filed(jack)
    var password: String? = null,  // Filed(1111)
    var statue: Int? = null

)