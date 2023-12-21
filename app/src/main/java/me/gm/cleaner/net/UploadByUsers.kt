package me.gm.cleaner.net

import org.json.JSONObject

object UploadByUsers {

    private suspend fun put(spec: String, data: JSONObject): Boolean = false

    suspend fun uploadAppTypeMarks(packageName: String, content: String): Boolean = false
}
