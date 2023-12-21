package me.gm.cleaner.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog
import me.gm.cleaner.settings.theme.ThemeUtil
import me.gm.cleaner.util.fileNameComparator
import me.gm.cleaner.util.fixEdgeEffect
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent
import java.io.File

class PathListPreferenceFragmentCompat : PreferenceDialogFragmentCompat() {
    private val pathListPreference: PathListPreference by lazy { preference as PathListPreference }
    private lateinit var adapter: PathListPreferenceAdapter
    internal var newValues: List<String> = emptyList()
        set(value) {
            field = value.distinct().sortedWith(fileNameComparator { it })
            adapter.submitList(field)
        }
    private val preferenceChanged: Boolean
        get() = pathListPreference.values != newValues.toSet()
    private var ignorePreferenceChanged: Boolean = false
    private val dialog: AlertDialog by lazy {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.quit_without_save)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ignorePreferenceChanged = true
                onDismiss(requireDialog())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        setStyle(STYLE_NORMAL, ThemeUtil.getColorThemeStyleRes())
        adapter = PathListPreferenceAdapter(this)
        newValues = if (savedInstanceState == null) {
            pathListPreference.values.toList()
        } else {
            if (savedInstanceState.getBoolean(SAVED_SHOWS_ALERT_DIALOG, false)) {
                dialog.show()
            }
            savedInstanceState.getStringArrayList(SAVE_STATE_VALUES)!!
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_SHOWS_ALERT_DIALOG, dialog.isShowing)
        outState.putStringArrayList(SAVE_STATE_VALUES, ArrayList(newValues))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                onDismiss(requireDialog())
            }
        }.apply {
            val contentView = onCreateDialogView(context)
            if (contentView != null) {
                onBindDialogView(contentView)
                setContentView(contentView)
            }
            val window = window ?: return@apply
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            window.setWindowAnimations(R.style.AppTheme_Slide)
        }

    @SuppressLint("RestrictedApi")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val appBarLayout = view.findViewById<AppBarLayout>(R.id.toolbar_container)
        view.findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { onDismiss(requireDialog()) }
            setNavigationIcon(R.drawable.ic_outline_close_24)
            SupportMenuInflater(context).inflate(R.menu.toolbar_save, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_save -> {
                        val dialog = requireDialog()
                        onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                        ignorePreferenceChanged = true
                        onDismiss(requireDialog())
                    }

                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            title = pathListPreference.dialogTitle
        }

        val list = view.findViewById<RecyclerView>(R.id.list)
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            FilePickerDialog()
                .apply {
                    setRoot(File.separator)
                    addOnPositiveButtonClickListener { dir ->
                        // newValues may need to be stored in the viewModel.
                        newValues += dir
                    }
                }
                .show(childFragmentManager, null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (preferenceChanged && !ignorePreferenceChanged) {
            this.dialog.show()
        } else {
            super.onDismiss(dialog)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && preferenceChanged) {
            val newValues = newValues.toSet()
            if (pathListPreference.callChangeListener(newValues)) {
                pathListPreference.values = newValues
            }
        }
    }

    companion object {
        private const val SAVE_STATE_VALUES: String = "PathListPreferenceFragmentCompat.values"
        private const val SAVED_SHOWS_ALERT_DIALOG: String = "android:showsAlertDialog"

        fun newInstance(key: String?): PathListPreferenceFragmentCompat =
            PathListPreferenceFragmentCompat().apply { arguments = bundleOf(ARG_KEY to key) }
    }
}
