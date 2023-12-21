package me.gm.cleaner.client.ui.storageredirect

class AppTypeMarks(yaml: Map<String, Any>, versionCode: Long) {
    @AppType
    val type: Int = when (yaml["type"]) {
        "Download" -> AppType.DOWNLOAD
        "AllFilesAccess" -> AppType.ALL_FILES_ACCESS
        "Common" -> AppType.COMMON
        else -> AppType.INVALID
    }

    val marks: List<String> by lazy {
        val allMarks = yaml["marks"] ?: return@lazy emptyList()
        var marksBestMatch: Map<String, Any>? = null
        var lastBestMatchVersionCode = -1L
        for (marks in allMarks as List<Map<String, Any>>) {
            val marksVersionCode = (marks["versionCode"] as Number).toLong()
            if (marksVersionCode > versionCode) {
                continue
            }
            if (marksBestMatch == null || marksVersionCode > lastBestMatchVersionCode) {
                marksBestMatch = marks
                lastBestMatchVersionCode = marksVersionCode
            }
        }
        val directories = marksBestMatch?.get("directories") ?: return@lazy emptyList()
        directories as List<String>
    }
}

@Retention(AnnotationRetention.SOURCE)
annotation class AppType {
    companion object {
        const val INVALID: Int = -1
        const val COMMON: Int = 0
        const val DOWNLOAD: Int = 1
        const val ALL_FILES_ACCESS: Int = 2
    }
}
