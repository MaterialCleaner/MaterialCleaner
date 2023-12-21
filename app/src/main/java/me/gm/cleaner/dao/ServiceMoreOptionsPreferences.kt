package me.gm.cleaner.dao

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import me.gm.cleaner.R
import me.gm.cleaner.client.ui.storageredirect.WizardAnswers
import me.gm.cleaner.util.toParcelable

object ServiceMoreOptionsPreferences {
    private lateinit var preferences: SharedPreferences
    private lateinit var res: Resources

    fun init(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        res = context.resources
    }

    val openWizardByDefault: Boolean
        get() = preferences.getBoolean(res.getString(R.string.open_wizard_by_default_key), false)

    val appsTypeMarksRepo: String
        get() = preferences.getString(
            res.getString(R.string.apps_type_marks_repo_key),
            res.getString(R.string.apps_type_marks_default)
        )!!

    val isUsingDefaultRepo: Boolean
        get() = appsTypeMarksRepo == res.getString(R.string.apps_type_marks_default)

    val autoCompleteByRecordMerge: Boolean
        get() = preferences.getBoolean(
            res.getString(R.string.auto_complete_by_record_merge_key), true
        )

    val autoCompleteByRecordRespect: Boolean
        get() = preferences.getBoolean(
            res.getString(R.string.auto_complete_by_record_respect_key), true
        )

    val editMountRulesTemplate: WizardAnswers
        get() = try {
            preferences.getString(res.getString(R.string.edit_mount_rules_template_key), null)
                ?.toParcelable()
                ?: WizardAnswers(true)
        } catch (e: Throwable) {
            TempCodeRecords.fixBug("2.0.1")
            preferences.edit {
                remove(res.getString(R.string.edit_mount_rules_template_key))
            }
            WizardAnswers(true)
        }

    val editReadOnlyTemplate: Set<String>
        get() = preferences.getStringSet(
            res.getString(R.string.edit_read_only_template_key), emptySet()
        )!!

    val applyTemplateOnPackageAdded: Boolean
        get() = preferences.getBoolean(
            res.getString(R.string.apply_template_on_package_added_key), false
        )
}
