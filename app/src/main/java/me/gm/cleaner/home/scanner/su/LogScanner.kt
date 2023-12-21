package me.gm.cleaner.home.scanner.su

import me.gm.cleaner.R
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.scanner.service.LogScannerService

class LogScanner(
    info: StaticScanner = StaticScanner(
        title = R.string.log,
        icon = R.drawable.ic_outline_log_24,
        scannerClass = LogScanner::class.java,
        viewModelClass = LogViewModel::class.java,
        serviceClass = LogScannerService::class.java
    )
) : BaseSuScanner(info)
