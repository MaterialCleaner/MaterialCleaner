package me.gm.cleaner.home

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import me.gm.cleaner.home.scanner.BaseScanner
import me.gm.cleaner.home.scanner.ScannerViewModel
import me.gm.cleaner.home.scanner.service.BaseScannerService
import me.gm.cleaner.home.scanner.sh.BaseShScanner

@Parcelize
data class StaticScanner(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val scannerClass: Class<out BaseScanner>?,
    val viewModelClass: Class<out ScannerViewModel>?,
    val serviceClass: Class<out BaseScannerService>?
) : Parcelable {

    fun newInstance(): BaseScanner =
        scannerClass!!.getConstructor(StaticScanner::class.java).newInstance(this)

    @IgnoredOnParcel
    val isShScanner: Boolean =
        scannerClass != null && BaseShScanner::class.java.isAssignableFrom(scannerClass)
}
