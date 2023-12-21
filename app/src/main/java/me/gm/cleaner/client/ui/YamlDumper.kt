package me.gm.cleaner.client.ui

object YamlDumper {

    fun dump(type: String, versionCode: Long, directories: List<String>): String =
        mutableListOf(
            "type: $type",
            "marks:",
            "  - versionCode: $versionCode",
            "    directories:",
        )
            .plus(
                directories.map { dir ->
                    "      - $dir"
                }
            )
            .joinToString("\n")
            .plus("\n")
}
