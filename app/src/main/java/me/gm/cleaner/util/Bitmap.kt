package me.gm.cleaner.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.annotation.Px
import kotlin.math.min

fun Bitmap.toScaledBitmap(
    @Px width: Int = this.width, @Px height: Int = this.height,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap {
    val bitmap = Bitmap.createBitmap(this.width, this.height, config)
    val src = Rect(0, 0, this.width, this.height)
    val dst = Rect(
        this.width / 2 - width / 2, this.height / 2 - height / 2,
        this.width / 2 + width / 2, this.height / 2 + height / 2
    )
    Canvas(bitmap).drawBitmap(this, src, dst, Paint())
    return bitmap
}

fun Bitmap.toRoundedBitmap(
    @Px size: Int = min(width, height), config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, config)
    val canvas = Canvas(bitmap)
    val radius = size.toFloat() / 2
    val paint = Paint()
    paint.isAntiAlias = true
    canvas.drawCircle(radius, radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    val src = Rect(
        (width / 2 - radius).toInt(), (height / 2 - radius).toInt(),
        (width / 2 + radius).toInt(), (height / 2 + radius).toInt()
    )
    val dst = Rect(0, 0, size, size)
    canvas.drawBitmap(this, src, dst, paint)
    return bitmap
}
