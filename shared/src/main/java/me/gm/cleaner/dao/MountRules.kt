package me.gm.cleaner.dao

import me.gm.cleaner.util.FileUtils

class MountRules {
    private lateinit var ruleZipped: List<Pair<String, String>>
    private lateinit var rule: Pair<List<String>, List<String>>

    constructor(ruleZipped: List<Pair<String, String>>) {
        this.ruleZipped = ruleZipped
    }

    constructor(rule: Pair<List<String>, List<String>>) {
        this.rule = rule
    }

    private fun ensureRuleZipped(): List<Pair<String, String>> {
        if (!::ruleZipped.isInitialized) {
            ruleZipped = rule.first.zip(rule.second)
        }
        return ruleZipped
    }

    private fun ensureRule(): Pair<List<String>, List<String>> {
        if (!::rule.isInitialized) {
            rule = ruleZipped.unzip()
        }
        return rule
    }

    val sources: List<String>
        get() = ensureRule().first

    val targets: List<String>
        get() = ensureRule().second

    fun isEmpty(): Boolean =
        ::ruleZipped.isInitialized && ruleZipped.isEmpty() ||
                ::rule.isInitialized && rule.first.isEmpty()

    // FOR SERVER
    val mountPoint: List<String>
        get() {
            val mkdirList = mutableListOf<String>()
            val mountedList = mutableListOf<Pair<String, String>>()
            ensureRuleZipped().forEach { (source, target) ->
                mkdirList.add(getMountedPath(mountedList, target))
                mountedList.add(Pair(source, target))
            }
            return mkdirList
        }

    // FOR UI
    val meaninglessRulesIndices: List<Int>
        get() {
            val ruleZipped = ensureRuleZipped()
            val indices = mutableListOf<Int>()
            for (i in targets.indices) {
                val target = targets[i]
                // overridden by subsequent mount rules
                if (targets.subList(i + 1, targets.size).any { FileUtils.startsWith(it, target) } ||
                    // makes no sense
                    getMountedPath(ruleZipped.subList(0, i), target) ==
                    getMountedPath(ruleZipped.subList(0, i + 1), target)
                ) {
                    indices += i
                }
            }
            return indices
        }

    // SHARED
    private fun getMountedPath(ruleZipped: List<Pair<String, String>>, path: String): String {
        val fileSystemLastMatch = ruleZipped.indexOfLast { (source, target) ->
            FileUtils.startsWith(target, path)
        }
        if (fileSystemLastMatch == -1) {
            // No matching target means the path is not changed by mounts.
            return path
        }

        var mountedPath = path
        ruleZipped.subList(fileSystemLastMatch, ruleZipped.size).forEach { (source, target) ->
            if (FileUtils.startsWith(target, mountedPath)) {
                mountedPath = source + mountedPath.substring(target.length)
            }
        }
        return mountedPath
    }

    fun getMountedPath(path: String): String = getMountedPath(ensureRuleZipped(), path)

    fun getAccessiblePlaces(path: String): List<String> {
        // Remove meaningless mount rules.
        val ruleZipped = ensureRuleZipped().toMutableList().apply {
            meaninglessRulesIndices.asReversed().forEach { index ->
                removeAt(index)
            }
        }
        val paths = mutableListOf<String>()
        // Check if the path is directly accessible.
        if (ruleZipped.unzip().second.none { FileUtils.startsWith(it, path) }) {
            paths += path
        }
        // Find matching file systems.
        for (i in ruleZipped.indices) {
            val (source, target) = ruleZipped[i]
            if (FileUtils.startsWith(source, path)) {
                val maybeAccessiblePath = target + path.substring(source.length)
                // If maybeAccessiblePath is not changed by subsequent mounts, it is accessible.
                if (maybeAccessiblePath ==
                    getMountedPath(ruleZipped.subList(i + 1, ruleZipped.size), maybeAccessiblePath)
                ) {
                    if (maybeAccessiblePath !in paths) {
                        paths += maybeAccessiblePath
                    }
                }
            }
        }
        return paths
    }
}
