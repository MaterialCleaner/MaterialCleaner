package me.gm.cleaner.dao

object TempCodeRecords {
    /**
     * ```
     *  try {
     *      // new code here
     *  } catch (e: Throwable) {
     *      TempCodeRecord.fixBug("versionName")
     *      // fix bug code here
     *      // Remove these codes should have no effect for new users.
     *  }
     * ```
     */
    fun fixBug(commitVersion: String, comment: String = "") {}
    fun advance(commitVersion: String, comment: String = "") {}
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class TempCode(val comment: String = "")
