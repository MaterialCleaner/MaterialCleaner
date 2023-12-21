package me.gm.cleaner.xposed

object InlineHookConfig {

    private external fun a(value: Array<String>)
    fun setMountPoint(value: Array<String>) = a(value)

    private external fun a(value: Boolean)
    fun setRecordExternalAppSpecificStorage(value: Boolean) = a(value)
}
