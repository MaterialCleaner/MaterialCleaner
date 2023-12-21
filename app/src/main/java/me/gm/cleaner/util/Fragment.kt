package me.gm.cleaner.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.startActivitySafe(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
    }
}
