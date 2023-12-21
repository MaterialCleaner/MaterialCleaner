package me.gm.cleaner.util

import java.text.Collator

private val collator: Collator by lazy { Collator.getInstance() }
private val naturalSorter: NaturalSorter by lazy { NaturalSorter() }

fun <T> collatorComparator(convert: (T) -> String): Comparator<T> {
    return Comparator { o1, o2 ->
        collator.compare(convert(o1), convert(o2))
    }
}

fun <T> fileNameComparator(convert: (T) -> String): Comparator<T> {
    return Comparator { o1, o2 ->
        naturalSorter.compare(convert(o1), convert(o2))
    }
}
