package me.gm.cleaner.client.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.dialog.InsetDialogOnTouchListener
import com.google.android.material.dialog.MaterialDialogs
import me.gm.cleaner.R
import me.gm.cleaner.client.ui.storageredirect.MountWizard
import me.gm.cleaner.client.ui.storageredirect.WizardAnswers
import me.gm.cleaner.dao.ServiceMoreOptionsPreferences
import me.gm.cleaner.dao.TempCodeRecords
import me.gm.cleaner.databinding.StorageRedirectCategoryMountWizardQuestionsBinding
import me.gm.cleaner.settings.theme.ThemeUtil
import me.gm.cleaner.util.*

class EditMountRulesTemplatePreferenceFragmentCompat : PreferenceDialogFragmentCompat() {
    private val editTemplatePreference: EditMountRulesTemplatePreference
            by lazy { preference as EditMountRulesTemplatePreference }
    private lateinit var scrollView: ScrollView
    private lateinit var binding: StorageRedirectCategoryMountWizardQuestionsBinding
    private lateinit var newValue: WizardAnswers
    private val preferenceChanged: Boolean
        get() = editTemplatePreference.value != newValue.toBase64String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, ThemeUtil.getColorThemeStyleRes())
        newValue = try {
            if (savedInstanceState == null) {
                editTemplatePreference.value
            } else {
                savedInstanceState.getString(SAVE_STATE_VALUE)!!
            }.toParcelable()
        } catch (e: Throwable) {
            TempCodeRecords.fixBug("2.0.1")
            ServiceMoreOptionsPreferences.editMountRulesTemplate
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVE_STATE_VALUE, newValue.toBase64String())
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog = super.onCreateDialog(savedInstanceState)
        val window = alertDialog.window!!
        /* {@link Window#getDecorView()} should be called before any changes are made to the Window
         * as it locks in attributes and affects layout. */
        val decorView = window.decorView
        val context = requireContext().createMaterialAlertDialogThemedContext()
        val background = context.materialDialogBackgroundDrawable()
        background.elevation = ViewCompat.getElevation(decorView)
        val backgroundInsets = context.materialDialogBackgroundInsets()

        val insetDrawable = MaterialDialogs.insetDrawable(background, backgroundInsets)
        window.setBackgroundDrawable(insetDrawable)
        decorView.setOnTouchListener(InsetDialogOnTouchListener(alertDialog, backgroundInsets))
        return alertDialog
    }

    override fun onCreateDialogView(context: Context): View {
        scrollView = ScrollView(context).apply {
            scrollIndicators = View.SCROLL_INDICATOR_TOP or View.SCROLL_INDICATOR_BOTTOM
        }
        binding = StorageRedirectCategoryMountWizardQuestionsBinding.inflate(
            LayoutInflater.from(context), scrollView, true
        )
        val dialogPreferredPadding = context
            .getDimenByAttr(com.google.android.material.R.attr.dialogPreferredPadding).toInt()
        binding.title.updatePaddingRelative(
            start = dialogPreferredPadding, end = dialogPreferredPadding
        )
        val paddingStart = dialogPreferredPadding -
                context.resources.getDimensionPixelSize(R.dimen.card_margin)
        val paddingSpace =
            context.resources.getDimensionPixelSize(R.dimen.icon_center_from_screen_edge) -
                    paddingStart
        arrayOf(binding.q1, binding.q2, binding.q3).forEach {
            val child = it.getChildAt(0) as ViewGroup
            child.getChildAt(0).updatePaddingRelative(start = paddingStart)
            child.getChildAt(1).updatePaddingRelative(
                start = paddingSpace, end = dialogPreferredPadding
            )
        }
        arrayOf(binding.widgetFrame1, binding.widgetFrame2).forEach {
            it.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = dialogPreferredPadding
            }
        }
        return scrollView
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        MountWizard.bindView(newValue, binding, this)
        return scrollView
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && preferenceChanged) {
            if (editTemplatePreference.callChangeListener(newValue)) {
                editTemplatePreference.value = newValue.toBase64String()
            }
        }
    }

    companion object {
        private const val SAVE_STATE_VALUE: String = "EditTemplatePreferenceFragmentCompat.value"

        fun newInstance(key: String?): EditMountRulesTemplatePreferenceFragmentCompat =
            EditMountRulesTemplatePreferenceFragmentCompat().apply {
                arguments = bundleOf(ARG_KEY to key)
            }
    }
}
