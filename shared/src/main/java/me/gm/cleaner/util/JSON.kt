package me.gm.cleaner.util

import org.json.JSONArray

fun JSONArray.toList(): ArrayList<String> {
    val list = ArrayList<String>()
    for (i in 0 until length()) {
        list.add(getString(i))
    }
    return list
}
