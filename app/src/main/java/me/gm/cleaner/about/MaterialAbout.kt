package me.gm.cleaner.about

import com.danielstone.materialaboutlibrary.items.MaterialAboutItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard

fun MaterialAboutCard.Builder.addItems(items: Iterable<MaterialAboutItem>)
        : MaterialAboutCard.Builder {
    items.forEach { item ->
        addItem(item)
    }
    return this
}
