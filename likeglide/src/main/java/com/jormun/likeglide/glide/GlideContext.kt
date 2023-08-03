package com.jormun.likeglide.glide

import android.content.Context
import com.jormun.likeglide.glide.load.Engine
import com.jormun.likeglide.glide.request.RequestOptions

/**
 * 封装了Glide的上下文环境的类
 * @param context 上下文
 * @param defaultRequestOptions 默认的加载配置信息对象
 * @param registry 注册类，包含了数据加载器和解码器
 * @param engine 执行加载任务的Engine
 */
class GlideContext(
    var context: Context, var defaultRequestOptions: RequestOptions,
    var registry: Registry, val engine: Engine
) {
}