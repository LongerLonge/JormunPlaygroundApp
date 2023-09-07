package com.jormun.likehttp.net

/**
 * Request请求封装对象，建造者模式创建
 * @param builder: 建造者模式创建
 */
class Request(builder: Builder) {

    //请求头
    var headers: Map<String, String>

    //请求方法，比如get\post
    var method: String

    //请求体(post需要)
    var body: RequestBody? = null

    //Url的封装类，见HttpUrl
    var url: HttpUrl

    /**
     * 根据当前的request信息重新构建一个builder
     * 可以用来快速生成新builder同时加入新参数，然后build一个新的request出来
     */
    fun newBuilder(): Builder = Builder(this)

    /**
     * 建造者模式
     */
    class Builder {

        val headers = mutableMapOf<String, String>()
        lateinit var method: String
        var body: RequestBody? = null
        lateinit var url: HttpUrl

        constructor()

        internal constructor(request: Request) {
            this.url = request.url
            this.method = request.method
            this.body = request.body
        }

        /**
         *往请求头添加一个数据(键值对)
         *@param key: key
         * @param value: value
         */
        fun addHeader(key: String, value: String): Builder {
            headers.put(key, value)
            return this
        }

        /**
         * 移除某个请求头
         * @param key: 指定的Key
         */
        fun removeHeader(key: String): Builder {
            headers.remove(key)
            return this
        }

        /**
         * 最基本的get请求
         */
        fun get(): Builder {
            method = "GET"
            return this
        }

        /**
         * post请求，可以带上body
         * @param body post请求需要的body
         */
        fun post(body: RequestBody?): Builder {
            method = "POST"
            this.body = body
            return this
        }

        /**
         * 可以自己手动设置body
         */
        fun requestBody(body: RequestBody): Builder {
            this.body = body
            return this
        }

        /**
         * 设置url
         * @param url: 需要请求的url
         */
        fun setUrl(url: String): Builder {
            try {
                this.url = HttpUrl(url)
                return this
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }

        /**
         * 创建Request对象
         */
        fun build(): Request {
            if (!this::url.isInitialized) {
                throw Exception("please set url first!")
            }
            if (!this::method.isInitialized) {
                throw Exception("please invoke put/get! ")
            } else if (method.isEmpty()) {
                method = "GET"
            }
            return Request(this)
        }
    }

    init {
        headers = builder.headers
        method = builder.method
        body = builder.body
        url = builder.url
    }
}