package com.jormun.likehttp.internal

import java.util.Locale

fun format(format: String, vararg args: Any): String {
    return String.format(Locale.US, format, *args)
}

fun isSensitiveHeader(name: String): Boolean {
    return name.equals("Authorization", ignoreCase = true) ||
            name.equals("Cookie", ignoreCase = true) ||
            name.equals("Proxy-Authorization", ignoreCase = true) ||
            name.equals("Set-Cookie", ignoreCase = true)
}