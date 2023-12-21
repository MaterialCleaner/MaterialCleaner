package me.gm.cleaner.net

import java.util.Locale
import java.util.function.Supplier

class LocaleAdapter(vararg localeToContentSuppliers: Pair<Locale, () -> String>) {
    private val supportedLocales: MutableMap<Locale, Supplier<String>> = mutableMapOf()

    fun addSupportedLocale(locale: Locale, contentSupplier: Supplier<String>): LocaleAdapter {
        supportedLocales[locale] = contentSupplier
        return this
    }

    fun addSupportedLocales(vararg localeToContentSuppliers: Pair<Locale, () -> String>): LocaleAdapter {
        localeToContentSuppliers.forEach { (locale, contentSupplier) ->
            addSupportedLocale(locale, contentSupplier)
        }
        return this
    }

    fun getContentForLocale(requestedLocale: Locale): String {
        supportedLocales[requestedLocale]?.let { return it.get() }

        supportedLocales.forEach { (locale, contentSupplier) ->
            if (requestedLocale.language == locale.language) {
                return contentSupplier.get()
            }
        }

        supportedLocales[Locale.US]?.let { return it.get() }

        throw NoSuchElementException("unsupported locale: $requestedLocale")
    }

    init {
        addSupportedLocales(*localeToContentSuppliers)
    }
}
