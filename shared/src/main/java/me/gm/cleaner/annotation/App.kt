package me.gm.cleaner.annotation

import android.annotation.SuppressLint
import java.lang.annotation.ElementType

/**
 * Restrict usage to code within app module.
 */
// Needed due to Kotlin's lack of PACKAGE annotation target
// https://youtrack.jetbrains.com/issue/KT-45921
@SuppressLint("SupportAnnotationUsage")
@Suppress("DEPRECATED_JAVA_ANNOTATION")
@java.lang.annotation.Target(
    ElementType.PACKAGE,
    ElementType.TYPE,
    ElementType.ANNOTATION_TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.METHOD,
    ElementType.FIELD
)
@Retention(AnnotationRetention.SOURCE)
annotation class App
