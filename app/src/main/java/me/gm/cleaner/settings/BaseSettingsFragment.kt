package me.gm.cleaner.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.settings.theme.ThemeColorPreference
import me.gm.cleaner.settings.theme.ThemeColorPreferenceDialogFragmentCompat
import me.gm.cleaner.util.fitsSystemWindowInsets
import me.gm.cleaner.util.fixEdgeEffect
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent

abstract class BaseSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreateRecyclerView(
        inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?
    ): RecyclerView {
        val list = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        list.fitsSystemWindowInsets()
        return list
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val f = when (preference) {
            is EditTextPreference -> {
                MaterialEditTextPreferenceDialogFragmentCompat.newInstance(preference.key)
            }

            is PathListPreference -> {
                PathListPreferenceFragmentCompat.newInstance(preference.key)
            }

            is ThemeColorPreference -> {
                ThemeColorPreferenceDialogFragmentCompat.newInstance(preference.key)
            }

            else -> {
                super.onDisplayPreferenceDialog(preference)
                return
            }
        }
        f.setTargetFragment(this, 0)
        f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
    }

    companion object {
        private const val DIALOG_FRAGMENT_TAG: String =
            "androidx.preference.PreferenceFragment.DIALOG"
    }
}
