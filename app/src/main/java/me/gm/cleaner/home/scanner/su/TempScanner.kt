package me.gm.cleaner.home.scanner.su

import me.gm.cleaner.R
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.scanner.service.TempScannerService

class TempScanner(
    info: StaticScanner = StaticScanner(
        title = R.string.temp,
        icon = R.drawable.ic_outline_temp_24,
        scannerClass = TempScanner::class.java,
        viewModelClass = TempViewModel::class.java,
        serviceClass = TempScannerService::class.java
    )
) : BaseSuScanner(info)
