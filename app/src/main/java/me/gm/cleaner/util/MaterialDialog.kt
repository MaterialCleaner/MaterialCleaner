package me.gm.cleaner.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.resources.MaterialAttributes
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.theme.overlay.MaterialThemeOverlay

@AttrRes
val DEF_STYLE_ATTR = R.attr.alertDialogStyle

@StyleRes
val DEF_STYLE_RES = R.style.MaterialAlertDialog_MaterialComponents

@AttrRes
val MATERIAL_ALERT_DIALOG_THEME_OVERLAY = R.attr.materialAlertDialogTheme

@SuppressLint("RestrictedApi")
private fun getMaterialAlertDialogThemeOverlay(context: Context): Int {
    val materialAlertDialogThemeOverlay =
        MaterialAttributes.resolve(context, MATERIAL_ALERT_DIALOG_THEME_OVERLAY) ?: return 0
    return materialAlertDialogThemeOverlay.data
}

fun Context.createMaterialAlertDialogThemedContext(): Context {
    val themeOverlayId = getMaterialAlertDialogThemeOverlay(this)
    val themedContext = MaterialThemeOverlay.wrap(this, null, DEF_STYLE_ATTR, DEF_STYLE_RES)
    if (themeOverlayId == 0) {
        return themedContext
    }
    return ContextThemeWrapper(themedContext, themeOverlayId)
}

@SuppressLint("RestrictedApi")
fun Context.materialDialogBackgroundInsets(): Rect = MaterialDialogs.getDialogBackgroundInsets(
    this, DEF_STYLE_ATTR, DEF_STYLE_RES
)

fun Context.materialDialogBackgroundDrawable(): MaterialShapeDrawable {
    val surfaceColor = MaterialColors.getColor(this, R.attr.colorSurface, javaClass.canonicalName)
    val materialShapeDrawable = MaterialShapeDrawable(this, null, DEF_STYLE_ATTR, DEF_STYLE_RES)
    materialShapeDrawable.initializeElevationOverlay(this)
    materialShapeDrawable.fillColor = ColorStateList.valueOf(surfaceColor)

    // dialogCornerRadius first appeared in Android Pie
    if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
        val dialogCornerRadiusValue = TypedValue()
        theme.resolveAttribute(android.R.attr.dialogCornerRadius, dialogCornerRadiusValue, true)
        val dialogCornerRadius = dialogCornerRadiusValue.getDimension(resources.displayMetrics)
        if (dialogCornerRadiusValue.type === TypedValue.TYPE_DIMENSION && dialogCornerRadius >= 0) {
            materialShapeDrawable.setCornerSize(dialogCornerRadius)
        }
    }
    return materialShapeDrawable
}
