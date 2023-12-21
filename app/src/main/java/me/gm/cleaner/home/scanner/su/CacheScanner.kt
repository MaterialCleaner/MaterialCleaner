package me.gm.cleaner.home.scanner.su

import me.gm.cleaner.R
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.scanner.service.CacheScannerService

class CacheScanner(
    info: StaticScanner = StaticScanner(
        title = R.string.cache,
        icon = R.drawable.ic_outline_cache_24,
        scannerClass = CacheScanner::class.java,
        viewModelClass = CacheViewModel::class.java,
        serviceClass = CacheScannerService::class.java
    )
) : BaseSuScanner(info)
