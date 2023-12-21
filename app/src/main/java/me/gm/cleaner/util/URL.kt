package me.gm.cleaner.util

import java.net.URLDecoder
import java.net.URLEncoder

fun String.decodeURL(charset: String = Charsets.UTF_8.toString()): String =
    URLDecoder.decode(this, charset)

fun String.encodeURL(charset: String = Charsets.UTF_8.toString()): String =
    URLEncoder.encode(this, charset)
