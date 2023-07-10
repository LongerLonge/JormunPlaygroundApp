package com.jormun.likeglide.glide.bean

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * 用一个弱引用来包装Resource
 */
final class ResourceWeakRef(
    val key: Key,
    resources: Resources,
    referenceQueue: ReferenceQueue<Resources>
) :
    WeakReference<Resources>(resources, referenceQueue)

