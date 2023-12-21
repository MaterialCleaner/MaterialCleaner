package me.gm.cleaner.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import java.util.function.Consumer

class ConfirmationViewModel(application: Application) : AndroidViewModel(application) {
    val onPositiveButtonClickListeners: MutableSet<Consumer<ConfirmationDialog>> = mutableSetOf()
    val onNegativeButtonClickListeners: MutableSet<Consumer<ConfirmationDialog>> = mutableSetOf()
}
