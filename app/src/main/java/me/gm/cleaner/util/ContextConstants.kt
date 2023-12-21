package me.gm.cleaner.util

import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.ColorInt

val Context.colorPrimary: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorPrimary)!!

val Context.colorPrimaryContainer: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorPrimaryContainer)!!

val Context.colorOnPrimaryContainer: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorOnPrimaryContainer)!!

val Context.colorAccent: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorAccent)!!

val Context.colorBackground: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorBackground)!!

val Context.colorBackgroundFloating: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorBackgroundFloating)!!

val Context.colorSurface: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorSurface)!!

val Context.colorError: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorError)!!

val Context.colorControlNormal: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorControlNormal)!!

val Context.colorControlHighlight: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorControlHighlight)!!

val Context.textColorPrimary: ColorStateList
    get() = getColorStateListByAttr(android.R.attr.textColorPrimary)!!

val Context.textColorPrimaryInverse: ColorStateList
    get() = getColorStateListByAttr(android.R.attr.textColorPrimaryInverse)!!

val Context.navAnimTime: Long
    get() = resources.getInteger(androidx.navigation.ui.R.integer.config_navAnimTime).toLong()

val Context.shortAnimTime: Long
    get() = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

val Context.mediumAnimTime: Long
    get() = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

val Context.longAnimTime: Long
    get() = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
