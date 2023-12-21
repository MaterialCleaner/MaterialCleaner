package me.gm.cleaner.client.ui.storageredirect

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore.Files.FileColumns
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.os.ParcelCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.InfiniteGridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FILE_AND_FOLDER
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FOLDER
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.client.getPathWithEvent
import me.gm.cleaner.client.getSharedUserIdPackages
import me.gm.cleaner.dao.AppLabelCache
import me.gm.cleaner.dao.MountRules
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.dao.ServiceMoreOptionsPreferences
import me.gm.cleaner.databinding.StorageRedirectCategoryMountButtonsWizardBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryMountRuleItemBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryMountWizardQuestionsAccessibleFoldersItemBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryMountWizardQuestionsBinding
import me.gm.cleaner.model.FileSystemEvent
import me.gm.cleaner.util.ClipboardUtils
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.OpenUtils
import me.gm.cleaner.util.PermissionUtils
import me.gm.cleaner.util.getResourceIdByAttr
import me.gm.cleaner.widget.CheckableLinearLayout
import me.gm.cleaner.widget.recyclerview.ChildItemAnimator
import me.gm.cleaner.widget.recyclerview.DiffArrayList
import me.gm.cleaner.widget.recyclerview.UpdatingList
import me.gm.cleaner.widget.recyclerview.submitDiffList
import java.io.File

data class WizardAnswers(
    var q1: Boolean = false,
    var q2: Boolean = false,
    var q3: Boolean = false,
    var q4: Boolean = false,
    var q11: Boolean = false,
    var q12: Boolean = false,
    val accessiblePlacesLiveData: MutableLiveData<DiffArrayList<String?>> =
        MutableLiveData(DiffArrayList(listOf(null))),
    val mountRulesLiveData: MutableLiveData<DiffArrayList<Pair<String?, String?>>> =
        MutableLiveData(DiffArrayList(listOf(null to null))),
    val inaccessiblePlacesLiveData: MutableLiveData<DiffArrayList<String?>> =
        MutableLiveData(DiffArrayList(listOf(null))),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        ParcelCompat.readBoolean(parcel),
        ParcelCompat.readBoolean(parcel),
        ParcelCompat.readBoolean(parcel),
        ParcelCompat.readBoolean(parcel),
        ParcelCompat.readBoolean(parcel),
        ParcelCompat.readBoolean(parcel),
        MutableLiveData(DiffArrayList(parcel.createStringArrayList()!!)),
        MutableLiveData(
            DiffArrayList(parcel.createStringArrayList()!!.zip(parcel.createStringArrayList()!!))
        ),
        MutableLiveData(DiffArrayList(parcel.createStringArrayList()!!))
    )

    fun accessiblePlaces(): List<String> =
        accessiblePlacesLiveData.value!!.run { subList(0, size - 1) } as List<String>

    fun updateAccessiblePlaces(action: DiffArrayList<String?>.() -> Unit) {
        accessiblePlacesLiveData.postValue(accessiblePlacesLiveData.value!!.also { action(it) })
    }

    fun mountRules(): List<Pair<String, String>> =
        mountRulesLiveData.value!!.run { subList(0, size - 1) } as List<Pair<String, String>>

    fun updateMountRules(action: DiffArrayList<Pair<String?, String?>>.() -> Unit) {
        mountRulesLiveData.postValue(mountRulesLiveData.value!!.also { action(it) })
    }

    fun inaccessiblePlaces(): List<String> =
        inaccessiblePlacesLiveData.value!!.run { subList(0, size - 1) } as List<String>

    fun updateInaccessiblePlaces(action: DiffArrayList<String?>.() -> Unit) {
        inaccessiblePlacesLiveData.postValue(inaccessiblePlacesLiveData.value!!.also { action(it) })
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        ParcelCompat.writeBoolean(parcel, q1)
        ParcelCompat.writeBoolean(parcel, q2)
        ParcelCompat.writeBoolean(parcel, q3)
        ParcelCompat.writeBoolean(parcel, q11)
        ParcelCompat.writeBoolean(parcel, q12)
        parcel.writeStringList(accessiblePlacesLiveData.value)
        val mountRulesUnzipped = mountRulesLiveData.value!!.unzip()
        parcel.writeStringList(mountRulesUnzipped.first)
        parcel.writeStringList(mountRulesUnzipped.second)
        parcel.writeStringList(inaccessiblePlacesLiveData.value)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WizardAnswers> {
        override fun createFromParcel(parcel: Parcel): WizardAnswers = WizardAnswers(parcel)
        override fun newArray(size: Int): Array<WizardAnswers?> = arrayOfNulls(size)
    }
}

data class AutoAnswerRationale(
    val recordCount: Int,
    val q1Reasons: List<FileSystemEvent>,
    val accessiblePlacesReasons: List<List<FileSystemEvent>>,
    val mountRulesReasons: List<List<FileSystemEvent>>,
    val inaccessiblePlacesReasons: List<FileSystemEvent>,
)

class MountWizard(private val packageInfo: PackageInfo) {
    private val packageName: String = packageInfo.packageName
    private val initialAnswers: WizardAnswers
        get() = WizardAnswers(
            q12 = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    packageInfo.applicationInfo.category and ApplicationInfo.CATEGORY_GAME != 0 ||
                    packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0) ||
                    CleanerClient.service!!.createFileModel(obbDir).isDirectory
        )

    // BIND
    private lateinit var wizardBinding: StorageRedirectCategoryMountWizardQuestionsBinding
    private lateinit var answers: WizardAnswers
    fun bindView(
        mountRules: List<Pair<String, String>>,
        binding: StorageRedirectCategoryMountWizardQuestionsBinding,
        fragment: StorageRedirectFragment
    ) {
        val isFirstTimeBinding = !::wizardBinding.isInitialized
        wizardBinding = binding
        if (isFirstTimeBinding) {
            answers = if (mountRules.isNotEmpty() && PurchaseVerification.isExpressPro) {
                retrodictAnswers(mountRules)
            } else {
                initialAnswers.copy()
            }
        }
        bindView(answers, binding, fragment)
    }

    fun bindButton(
        binding: StorageRedirectCategoryMountButtonsWizardBinding,
        fragment: StorageRedirectFragment,
        appTypeMarks: () -> AppTypeMarks?,
    ) {
        binding.autoCompleteByRecord.isVisible = CleanerClient.zygiskEnabled
        binding.autoCompleteByRecord.setOnClickListener {
            fragment.lifecycleScope.launch {
                val record = withContext(Dispatchers.IO) {
                    CleanerClient.service!!.queryDistinctRecordsInclude(arrayOf(packageName)).list
                }
                val rationale = withContext(Dispatchers.Default) {
                    answerBasedOnRecord(answers, record, appTypeMarks())
                }
                val title = fragment.getString(
                    R.string.auto_complete_by_record_rationale_title, rationale.recordCount
                )
                val rationales = arrayListOf<Pair<String, String>>()
                if (rationale.q1Reasons.isNotEmpty()) {
                    rationales += fragment.getString(
                        R.string.auto_complete_by_record_rationale_content_header,
                        fragment.getString(R.string.storage_redirect_question_1)
                    ) to rationale.q1Reasons.joinToString("\n") { event ->
                        getPathWithEvent(event.path, event.flags, fragment)
                    }
                }
                if (rationale.accessiblePlacesReasons.isNotEmpty()) {
                    rationales += fragment.getString(
                        R.string.auto_complete_by_record_rationale_content_header,
                        fragment.getString(R.string.storage_redirect_question_2)
                    ) to rationale.accessiblePlacesReasons.mapIndexed { index, list ->
                        index.toString() + "\n" + list.joinToString("\n") { event ->
                            getPathWithEvent(event.path, event.flags, fragment)
                        }
                    }.joinToString("\n")
                }
                if (rationale.mountRulesReasons.isNotEmpty()) {
                    rationales += fragment.getString(
                        R.string.auto_complete_by_record_rationale_content_header,
                        fragment.getString(R.string.storage_redirect_question_3)
                    ) to rationale.mountRulesReasons.mapIndexed { index, list ->
                        index.toString() + "\n" + list.joinToString("\n") { event ->
                            getPathWithEvent(event.path, event.flags, fragment)
                        }
                    }.joinToString("\n")
                }
                if (rationale.inaccessiblePlacesReasons.isNotEmpty()) {
                    rationales += fragment.getString(
                        R.string.auto_complete_by_record_rationale_content_header,
                        fragment.getString(R.string.storage_redirect_question_4)
                    ) to rationale.inaccessiblePlacesReasons.joinToString("\n") { event ->
                        getPathWithEvent(event.path, event.flags, fragment)
                    }
                }
                RationaleDialog
                    .newInstance(title, rationales)
                    .show(fragment.childFragmentManager, null)
                if (::wizardBinding.isInitialized) {
                    restoreCheckedState(wizardBinding, answers)
                }
            }
        }
    }

    companion object {

        fun bindView(
            answers: WizardAnswers,
            binding: StorageRedirectCategoryMountWizardQuestionsBinding,
            fragment: Fragment
        ) {
            fun CheckableLinearLayout.maybePost(
                adapter: BaseKtListAdapter<*, *>, newSize: Int, action: Runnable
            ) {
                val oldSize = adapter.currentList.size
                if (isChecked || newSize <= 1) {
                    action.run()
                } else {
                    if (oldSize <= 1) {
                        // workaround item not rebound when child hidden
                        adapter.notifyItemChanged(0)
                    }
                    postOnAnimation(action)
                }
            }

            val context = fragment.requireContext()
            val inflater = LayoutInflater.from(context)
            val switchWidgetLayout = context.getResourceIdByAttr(R.attr.switchWidgetLayoutStyle)
            inflater.inflate(switchWidgetLayout, binding.widgetFrame1)
            inflater.inflate(switchWidgetLayout, binding.widgetFrame2)
            restoreCheckedState(binding, answers)

            binding.q1.setOnClickListener {
                answers.q1 = binding.q1.isChecked
            }
            binding.q11.setOnClickListener {
                answers.q11 = binding.q11.isChecked
            }
            binding.q12.setOnClickListener {
                answers.q12 = binding.q12.isChecked
            }

            binding.q2.setOnClickListener {
                answers.q2 = binding.q2.isChecked
            }
            val q2List = binding.q2List
            val q2Adapter = PathsAdapter(fragment, answers, SELECT_FOLDER)
            q2List.adapter = q2Adapter
            q2List.layoutManager = InfiniteGridLayoutManager(fragment.requireContext(), 1)
            q2List.setHasFixedSize(true)
            q2List.itemAnimator = ChildItemAnimator(q2List)
            answers.accessiblePlacesLiveData.removeObservers(fragment.viewLifecycleOwner)
            answers.accessiblePlacesLiveData.observe(fragment.viewLifecycleOwner) { accessiblePlaces ->
                binding.q2.maybePost(q2Adapter, accessiblePlaces.size) {
                    q2Adapter.submitDiffList(accessiblePlaces)
                }
            }

            binding.q3.setOnClickListener {
                answers.q3 = binding.q3.isChecked
            }
            val q3List = binding.q3List
            val q3Adapter = RedirectAdapter(fragment, answers)
            q3List.adapter = q3Adapter
            q3List.layoutManager = InfiniteGridLayoutManager(fragment.requireContext(), 1)
            q3List.setHasFixedSize(true)
            q3List.itemAnimator = ChildItemAnimator(q3List)
            answers.mountRulesLiveData.removeObservers(fragment.viewLifecycleOwner)
            answers.mountRulesLiveData.observe(fragment.viewLifecycleOwner) { mountRules ->
                binding.q3.maybePost(q3Adapter, mountRules.size) {
                    q3Adapter.submitDiffList(mountRules)
                }
            }

            binding.q4.setOnClickListener {
                answers.q4 = binding.q4.isChecked
            }
            val q4List = binding.q4List
            val q4Adapter = PathsAdapter(fragment, answers, SELECT_FILE_AND_FOLDER)
            q4List.adapter = q4Adapter
            q4List.layoutManager = InfiniteGridLayoutManager(fragment.requireContext(), 1)
            q4List.setHasFixedSize(true)
            q4List.itemAnimator = ChildItemAnimator(q4List)
            answers.inaccessiblePlacesLiveData.removeObservers(fragment.viewLifecycleOwner)
            answers.inaccessiblePlacesLiveData.observe(fragment.viewLifecycleOwner) { inaccessiblePlaces ->
                binding.q4.maybePost(q4Adapter, inaccessiblePlaces.size) {
                    q4Adapter.submitDiffList(inaccessiblePlaces)
                }
            }
        }

        private fun restoreCheckedState(
            binding: StorageRedirectCategoryMountWizardQuestionsBinding, answers: WizardAnswers
        ) {
            binding.q1.setCheckedNoAnim(answers.q1)
            binding.q2.setCheckedNoAnim(answers.q2)
            binding.q3.setCheckedNoAnim(answers.q3)
            binding.q4.setCheckedNoAnim(answers.q4)
            binding.q11.isChecked = answers.q11
            binding.q12.isChecked = answers.q12
        }
    }

    // SUBMIT
    private val sdDir: String = FileUtils.externalStorageDir.path
    private val dataDir: String = FileUtils.androidDataDir.resolve(packageName).path
    private val mediaDir: String = FileUtils.androidMediaDir.resolve(packageName).path
    private val obbDir: String = FileUtils.androidObbDir.resolve(packageName).path
    private val sandboxDir: String = FileUtils.androidSandboxDir.resolve(packageName).path
    private val filesDir: String = File(dataDir, "files").path
    private val cacheDir: String = File(dataDir, "cache").path

    fun createRules(): List<Pair<String, String>> = createRules(answers)

    private val mountRulesForMakingPathInaccessible: MountRules =
        MountRules(mutableListOf(cacheDir to sdDir))

    fun createRules(answers: WizardAnswers): List<Pair<String, String>> {
        val rules = mutableListOf<Pair<String, String>>()
        if (answers.q1) {
            rules += if (answers.q11) {
                filesDir to sdDir
            } else {
                cacheDir to sdDir
            }
            rules += dataDir to dataDir
            if (answers.q12) {
                rules += obbDir to obbDir
            }
        }
        if (answers.q2) {
            answers.accessiblePlaces().forEach { dir ->
                rules += dir to dir
            }
        }
        if (answers.q3) {
            answers.mountRules().forEach { (source, target) ->
                rules += source to target
            }
        }
        if (answers.q4) {
            val previousMountRules = MountRules(rules)
            answers.inaccessiblePlaces().forEach { dir ->
                previousMountRules.getAccessiblePlaces(dir).forEach { accessiblePlace ->
                    rules += mountRulesForMakingPathInaccessible
                        .getMountedPath(accessiblePlace) to accessiblePlace
                }
            }
        }
        if (answers.q1) {
            getSharedUserIdPackages(packageInfo)
                .asSequence()
                .filter { it.packageName != packageName }
                .forEach { otherPackageInfo ->
                    val otherDataDir =
                        FileUtils.androidDataDir.resolve(otherPackageInfo.packageName).path
                    if (!rules.contains(otherDataDir to otherDataDir)) {
                        rules += otherDataDir to otherDataDir
                    }
                    if (answers.q12) {
                        val otherObbDir =
                            FileUtils.androidObbDir.resolve(otherPackageInfo.packageName).path
                        if (!rules.contains(otherObbDir to otherObbDir)) {
                            rules += otherObbDir to otherObbDir
                        }
                    }
                }
        }
        return rules
    }

    fun retrodictAnswers(mountRules: List<Pair<String, String>>): WizardAnswers {
        val answers = WizardAnswers()
        var rulesNotBacktracked = mountRules
        // backtrack q1
        var q1Size = 0
        if (rulesNotBacktracked.size >= 2 && rulesNotBacktracked[1] == dataDir to dataDir) {
            if (rulesNotBacktracked[0] == cacheDir to sdDir) {
                answers.q1 = true
            } else if (rulesNotBacktracked[0] == filesDir to sdDir) {
                answers.q1 = true
                answers.q11 = true
            }
            if (answers.q1) {
                q1Size += 2
                if (rulesNotBacktracked.size >= 3 && rulesNotBacktracked[2] == obbDir to obbDir) {
                    answers.q12 = true
                    q1Size++
                }
            }
        }
        rulesNotBacktracked = rulesNotBacktracked.subList(q1Size, rulesNotBacktracked.size)

        // backtrack q2
        val q2Backtracked = mutableListOf<String>()
        for ((source, target) in rulesNotBacktracked) {
            if (source == target) {
                q2Backtracked += target
            } else {
                break
            }
        }
        if (q2Backtracked.isNotEmpty()) {
            answers.q2 = true
            answers.updateAccessiblePlaces {
                addAll(0, q2Backtracked)
            }
            rulesNotBacktracked =
                rulesNotBacktracked.subList(q2Backtracked.size, rulesNotBacktracked.size)
        }

        // backtrack q4
        val reversed = rulesNotBacktracked.asReversed().toMutableList().listIterator()
        val q4Backtracked = mutableListOf<String>()
        while (reversed.hasNext()) {
            val (source, target) = reversed.next()
            if (source == mountRulesForMakingPathInaccessible.getMountedPath(target)) {
                q4Backtracked += target
                reversed.remove()
            } else {
                reversed.previous()
                break
            }
        }
        val previousMountRules = MountRules(
            mountRules.subList(0, mountRules.size - q4Backtracked.size)
        )
        if (q4Backtracked.isNotEmpty()) {
            answers.q4 = true
            answers.updateInaccessiblePlaces {
                addAll(
                    0,
                    q4Backtracked
                        .map { path -> previousMountRules.getMountedPath(path) }
                        .distinct()
                        .asReversed()
                )
            }
            rulesNotBacktracked =
                rulesNotBacktracked.subList(0, rulesNotBacktracked.size - q4Backtracked.size)
        }

        // backtrack q3
        if (rulesNotBacktracked.isNotEmpty()) {
            answers.q3 = true
            for ((source, target) in rulesNotBacktracked) {
                answers.updateMountRules {
                    add(size - 1, source to target)
                }
            }
        }
        return answers
    }

    private val speciallyAllowedDirs: List<String> by lazy {
        val speciallyAllowedDirs = mutableSetOf<String>()
        FileUtils.defaultExternalNoScan.forEach { dir ->
            var parent = dir
            do {
                if (!speciallyAllowedDirs.add(parent.path)) {
                    break
                }
                parent = parent.parentFile!!
            } while (FileUtils.startsWith(FileUtils.externalStorageDirParent, parent))
        }
        speciallyAllowedDirs.toList()
    }

    fun answerBasedOnRecord(
        answers: WizardAnswers, record: List<FileSystemEvent>, appTypeMarks: AppTypeMarks?
    ): AutoAnswerRationale {
        // load record
        val hasStoragePermissions = PermissionUtils.containsStoragePermissions(packageInfo)
        val label = AppLabelCache.getPackageLabel(packageInfo)
        var usefulRecords = record.asSequence()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Writing files inside androidDataDir or androidObbDir will never succeed on R,
            // despite whether apps have storage permission.
            usefulRecords = usefulRecords.filterNot { event ->
                val path = FileUtils.getPathAsUser(event.path, 0)
                arrayOf(FileUtils.androidDataDir, FileUtils.androidObbDir).any { dir ->
                    FileUtils.startsWith(dir, path)
                }
            }
        }
        if (!hasStoragePermissions) {
            // If an app doesn't have storage permission,
            // writing files outside of standard dirs will never succeed.
            val standardParents = FileUtils.standardDirs
                .map { File(sdDir, it) } + FileUtils.androidDir
            usefulRecords = usefulRecords.filter { event ->
                val path = FileUtils.getPathAsUser(event.path, 0)
                standardParents.any { standardDir ->
                    FileUtils.startsWith(standardDir, path)
                }
            }
        }
        if (appTypeMarks != null && ServiceMoreOptionsPreferences.autoCompleteByRecordRespect) {
            when (appTypeMarks.type) {
                AppType.DOWNLOAD -> {
                    val marks = appTypeMarks.marks
                    usefulRecords = usefulRecords.filterNot { event ->
                        val path = FileUtils.getPathAsUser(event.path, 0)
                        marks.any { mark -> FileUtils.startsWith(mark, path) }
                    }
                }

                AppType.ALL_FILES_ACCESS -> {
                    usefulRecords = usefulRecords.filter { false } // clear()
                }
            }
        }
        usefulRecords = usefulRecords
            // allow writing files inside obbDir and mediaDir
            .filterNot { event ->
                val path = FileUtils.getPathAsUser(event.path, 0)
                FileUtils.startsWith(mediaDir, path) ||
                        FileUtils.startsWith(obbDir, path) ||
                        FileUtils.startsWith(sandboxDir, path)
            }
            // allow create speciallyAllowedDirs
            .filterNot { event ->
                val path = FileUtils.getPathAsUser(event.path, 0)
                event.flags and 0x40000000 != 0 && event.flags and FileObserver.CREATE != 0 &&
                        speciallyAllowedDirs.any { it.equals(path, true) }
            }
        val partition = usefulRecords.partition { event ->
            event.flags and 0x40000000 != 0
        }
        val dirRecords = partition.first
        val fileRecords = partition.second
        val q1Reasons = mutableListOf<FileSystemEvent>()
        val accessiblePlaces = mutableListOf<String>()
        val accessiblePlacesReasons = mutableListOf<MutableList<FileSystemEvent>>()
        val mountRules = mutableListOf<Pair<String, String>>()
        val mountRulesReasons = mutableListOf<MutableList<FileSystemEvent>>()
        // fileRecords
        // This can't be refactored with groupBy() because we need to extract reason.
        val mediaPaths = SparseArray<MutableList<FileSystemEvent>>()
        fileRecords.forEach { event ->
            when (val mediaType =
                MimeUtils.resolveMediaType(MimeUtils.resolveMimeType(File(event.path)))) {
                FileColumns.MEDIA_TYPE_AUDIO,
                FileColumns.MEDIA_TYPE_VIDEO,
                FileColumns.MEDIA_TYPE_IMAGE/*,
                FileColumns.MEDIA_TYPE_DOCUMENT*/ -> {
                    val events = mediaPaths[mediaType] ?: mutableListOf()
                    events += event
                    mediaPaths.put(mediaType, events)
                }

                else -> if (getRecommendDirs(mediaType)
                        .none { dir -> FileUtils.startsWith(dir, event.path) }
                ) {
                    q1Reasons += event
                }
            }
        }
        arrayOf(
            FileColumns.MEDIA_TYPE_AUDIO to Environment.DIRECTORY_MUSIC,
            FileColumns.MEDIA_TYPE_VIDEO to Environment.DIRECTORY_MOVIES,
            FileColumns.MEDIA_TYPE_IMAGE to Environment.DIRECTORY_PICTURES,
//            FileColumns.MEDIA_TYPE_DOCUMENT to Environment.DIRECTORY_DOCUMENTS,
        ).forEach { (mediaType, publicDir) ->
            val events = mediaPaths[mediaType] ?: emptyList()
            events.forEach { event ->
                val path = FileUtils.getPathAsUser(event.path, 0)
                val recommendDir = getRecommendDirs(mediaType)
                    .firstOrNull { dir -> FileUtils.startsWith(dir, path) }
                if (recommendDir != null) {
                    val mergeIndex = accessiblePlaces.indexOfFirst { dir ->
                        FileUtils.startsWith(dir, path)
                    }
                    if (mergeIndex == -1) {
                        accessiblePlaces += recommendDir.path
                        accessiblePlacesReasons += mutableListOf(event)
                    } else {
                        accessiblePlacesReasons[mergeIndex] += event
                    }
                } else if (
                // ignore open event
                    event.flags and FileObserver.OPEN == 0 ||
                    event.flags and FileObserver.CREATE != 0
                ) {
                    val redirectSource = sdDir + File.separator +
                            publicDir + File.separator + label
                    var parent = File(path)
                    var mergeIndex: Int
                    do {
                        parent = parent.parentFile!!
                        mergeIndex = mountRules.indexOfFirst { (source, target) ->
                            redirectSource == source && FileUtils.startsWith(parent, target)
                        }
                    } while (mergeIndex == -1 &&
                        FileUtils.childOf(sdDir, parent.parent!!)
                    )
                    if (mergeIndex == -1) {
                        mountRules += redirectSource to File(path).parent!!
                        mountRulesReasons += mutableListOf(event)
                    } else {
                        mountRules[mergeIndex] = redirectSource to parent.path
                        mountRulesReasons[mergeIndex] += event
                    }
                }
            }
        }
        // If a dir is redirected to multiple dirs,
        // redirect it to Download dir and remove the other redirections.
        val targetsNeedMerge = mountRules.groupBy { (source, target) -> target }
            .filter { it.value.size > 1 }
            .keys
        val targetsNeedMergeToReasons = targetsNeedMerge
            .associateWith { mutableListOf<FileSystemEvent>() }
        val indicesNeedMerge = mountRules
            .mapIndexedNotNull { index, (source, target) ->
                if (target in targetsNeedMerge) index else null
            }
            .asReversed()
            .forEach { index ->
                val target = mountRules.removeAt(index).second
                targetsNeedMergeToReasons[target]!! += mountRulesReasons.removeAt(index)
            }
        val downloadDir = sdDir + File.separator +
                Environment.DIRECTORY_DOWNLOADS + File.separator + label
        mountRules += targetsNeedMergeToReasons.keys.map { target -> downloadDir to target }
        mountRulesReasons += targetsNeedMergeToReasons.values
        // If disallowed file in q1Reasons is allowed in accessiblePlaces, disallow them in inaccessiblePlaces.
        val inaccessiblePlaces = mutableListOf<String>()
        val inaccessiblePlacesReasons = mutableListOf<FileSystemEvent>()
        val q1Iterator = q1Reasons.iterator()
        while (q1Iterator.hasNext()) {
            val event = q1Iterator.next()
            val path = FileUtils.getPathAsUser(event.path, 0)
            if (accessiblePlaces.any { dir -> FileUtils.childOf(dir, path) }) {
                val mergeIndex = inaccessiblePlaces.indexOfFirst { dir ->
                    FileUtils.startsWith(dir, path) || FileUtils.startsWith(path, dir)
                }
                if (mergeIndex == -1) {
                    inaccessiblePlaces += path
                } else {
                    if (FileUtils.childOf(path, inaccessiblePlaces[mergeIndex])) {
                        inaccessiblePlaces[mergeIndex] = path
                    }
                }
                inaccessiblePlacesReasons += event
                q1Iterator.remove()
            }
        }
        // dirRecords
        dirRecords.forEach { event ->
            val path = FileUtils.getPathAsUser(event.path, 0)
            if (mountRules.none { (source, target) -> FileUtils.startsWith(target, path) } &&
                accessiblePlaces.none { dir -> FileUtils.startsWith(dir, path) }
            ) {
                q1Reasons += event
            }
        }
        // result
        if (appTypeMarks != null && ServiceMoreOptionsPreferences.autoCompleteByRecordRespect) {
            val marks = appTypeMarks.marks
            when (appTypeMarks.type) {
                AppType.DOWNLOAD -> {
                    val targets = answers.mountRules().unzip().second
                    mountRules += marks
                        .filter { mark ->
                            targets.none { target -> FileUtils.startsWith(mark, target) }
                        }
                        .map { mark -> downloadDir to mark }
                }

                AppType.ALL_FILES_ACCESS -> {
                    answers.q1 = false
                    inaccessiblePlaces += marks
                }
            }
        }
        if (!ServiceMoreOptionsPreferences.autoCompleteByRecordMerge) {
            answers.q1 = q1Reasons.isNotEmpty()
            answers.q12 = initialAnswers.q12

            answers.q2 = accessiblePlaces.isNotEmpty()
            answers.updateAccessiblePlaces {
                clear()
                addAll(accessiblePlaces)
                add(null)
            }

            answers.q3 = mountRules.isNotEmpty()
            answers.updateMountRules {
                clear()
                addAll(mountRules)
                add(null to null)
            }

            answers.q4 = inaccessiblePlaces.isNotEmpty()
            answers.updateInaccessiblePlaces {
                clear()
                addAll(inaccessiblePlaces)
                add(null)
            }
        } else {
            answers.q1 = answers.q1 || q1Reasons.isNotEmpty()
            answers.q12 = answers.q12 || initialAnswers.q12

            answers.q2 = answers.q2 || accessiblePlaces.isNotEmpty()
            answers.updateAccessiblePlaces {
                addAll(size - 1, accessiblePlaces.filter { it !in this })
            }

            answers.q3 = answers.q3 || mountRules.isNotEmpty()
            answers.updateMountRules {
                addAll(size - 1, mountRules.filter { it !in this })
            }

            answers.q4 = answers.q4 || inaccessiblePlaces.isNotEmpty()
            answers.updateInaccessiblePlaces {
                addAll(size - 1, inaccessiblePlaces.filter { it !in this })
            }
        }
        return AutoAnswerRationale(
            CleanerClient.service!!.countRecordsInclude(arrayOf(packageName)),
            q1Reasons, accessiblePlacesReasons, mountRulesReasons, inaccessiblePlacesReasons
        )
    }

    fun getRecommendDirs(mediaType: Int): List<File> {
        val recommendDirs = when (mediaType) {
            FileColumns.MEDIA_TYPE_PLAYLIST -> FileUtils.standardDirs.map {
                File(sdDir, it)
            }

            FileColumns.MEDIA_TYPE_SUBTITLE -> FileUtils.standardDirs.map {
                File(sdDir, it)
            }

            FileColumns.MEDIA_TYPE_AUDIO -> mutableListOf(
                File(sdDir, Environment.DIRECTORY_MUSIC),
                File(sdDir, Environment.DIRECTORY_PODCASTS),
                File(sdDir, Environment.DIRECTORY_NOTIFICATIONS),
                File(sdDir, Environment.DIRECTORY_ALARMS),
                File(sdDir, Environment.DIRECTORY_RINGTONES),
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(File(sdDir, Environment.DIRECTORY_AUDIOBOOKS))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(File(sdDir, Environment.DIRECTORY_RECORDINGS))
                }
            }

            FileColumns.MEDIA_TYPE_VIDEO -> listOf(
                File(sdDir, Environment.DIRECTORY_MOVIES),
            )

            FileColumns.MEDIA_TYPE_IMAGE -> listOf(
                File(sdDir, Environment.DIRECTORY_PICTURES),
                File(sdDir, Environment.DIRECTORY_DCIM),
            )

            FileColumns.MEDIA_TYPE_DOCUMENT -> listOf(
                File(sdDir, Environment.DIRECTORY_DOCUMENTS),
            )

            FileColumns.MEDIA_TYPE_NONE -> listOf(
                File(sdDir, Environment.DIRECTORY_DOWNLOADS),
            )

            else -> throw AssertionError()
        }.toMutableList()
        val downloadDir = File(sdDir, Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir !in recommendDirs) {
            // Any type saved to Download dir is allowed.
            recommendDirs += downloadDir
        }
        return recommendDirs
    }

    fun getRecommendDirOps(
        oldList: List<Pair<String, String>>, newList: List<Pair<String, String>>
    ): List<DirOp> {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]
                return oldItem.first == newItem.first || oldItem.second == newItem.second
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]
                return oldItem == newItem
            }
        }, false)
        val dirsNeedMigrate = mutableSetOf<String>()
        val dirOps = mutableListOf<DirOp>()
        result.dispatchUpdatesTo(object : UpdatingList<Pair<String, String>>(oldList, newList) {
            override fun onInserted(list: List<Pair<String, String>>) {
                dirsNeedMigrate += list.unzip().second
            }

            override fun onRemoved(list: List<Pair<String, String>>) {
                dirsNeedMigrate += list.unzip().second
            }

            override fun onChanged(
                originList: List<Pair<String, String>>, changedList: List<Pair<String, String>>
            ) {
                changedList.forEachIndexed { index, pair ->
                    if (originList[index].first == pair.first) {
                        dirsNeedMigrate += pair.second
                    } else {
                        dirOps += DirOp.create(originList[index].first, pair.first, packageName)
                    }
                }
            }
        })
        val oldMountRules = MountRules(oldList)
        val newMountRules = MountRules(newList)
        return dirsNeedMigrate.asSequence()
            .map { dir ->
                val oldMountedDir = oldMountRules.getMountedPath(dir)
                val newMountedDir = newMountRules.getMountedPath(dir)
                DirOp.create(oldMountedDir, newMountedDir, packageName)
            }
            .plus(dirOps)
            .filterNot {
                sdDir == it.from || FileUtils.isStandardDirectory(
                    it.from.substring(sdDir.length + File.separator.length)
                )
            }
            .filterNot {
                FileUtils.startsWith(it.from, it.to)
            }
            .distinct()
            .filter {
                // validate dir exist
                CleanerClient.service!!.createFileModel(it.from).isDirectory
            }
            .toList()
    }

    sealed class DirOp(val from: String, val to: String, val checkByDefault: Boolean) {
        class Move(from: String, to: String, checkByDefault: Boolean) :
            DirOp(from, to, checkByDefault)

        class Copy(from: String, to: String, checkByDefault: Boolean) :
            DirOp(from, to, checkByDefault)

        override fun hashCode(): Int {
            var result = from.hashCode()
            result = 31 * result + to.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DirOp) return false

            if (from != other.from) return false
            if (to != other.to) return false

            return true
        }

        companion object {

            fun create(from: String, to: String, packageName: String): DirOp {
                val oldDirInternal = FileUtils.isKnownAppDirPaths(from, packageName)
                val newDirInternal = FileUtils.isKnownAppDirPaths(to, packageName)
                val oldDirExternal = !oldDirInternal
                val newDirExternal = !newDirInternal
                return when {
                    oldDirInternal && newDirInternal -> Move(from, to, true)
                    oldDirExternal && newDirExternal -> Move(from, to, true)
                    oldDirInternal && newDirExternal -> Move(from, to, false)
                    oldDirExternal && newDirInternal -> Move(from, to, true)
                    else -> throw AssertionError()
                }
            }
        }
    }
}

class RedirectAdapter(
    private val fragment: Fragment, private val answers: WizardAnswers
) : BaseKtListAdapter<Pair<String?, String?>, RedirectAdapter.ViewHolder>(CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryMountRuleItemBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val rule = getItem(position)
        binding.text1.text = rule.first
        binding.text1.isVisible = rule.first != null
        binding.add1.isVisible = rule.first == null
        binding.text2.text = rule.second
        binding.text2.isVisible = rule.second != null
        binding.add2.isVisible = rule.second == null
        binding.frame1.setOnClickListener {
            FilePickerDialog()
                .apply {
                    if (rule.first != null) {
                        setPath(rule.first!!)
                    }
                    setSelectType(SELECT_FOLDER)
                    addOnPositiveButtonClickListener { dir ->
                        val position = holder.bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            answers.updateMountRules {
                                set(position, dir to rule.second)
                                if (position == itemCount - 1 && rule.second != null) {
                                    add(null to null)
                                }
                            }
                        }
                    }
                }
                .show(fragment.childFragmentManager, null)
        }
        binding.frame2.setOnClickListener {
            FilePickerDialog()
                .apply {
                    if (rule.second != null) {
                        setPath(rule.second!!)
                    }
                    setSelectType(SELECT_FOLDER)
                    addOnPositiveButtonClickListener { dir ->
                        val position = holder.bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            answers.updateMountRules {
                                set(position, rule.first to dir)
                                if (position == itemCount - 1 && rule.first != null) {
                                    add(null to null)
                                }
                            }
                        }
                    }
                }
                .show(fragment.childFragmentManager, null)
        }
        binding.frame1.setOnCreateContextMenuListener { menu, _, _ ->
            if (rule.first != null) {
                fragment.requireActivity().menuInflater.inflate(
                    R.menu.mount_rules_editor_items, menu
                )
                val bindingAdapterPosition = holder.bindingAdapterPosition
                menu.setHeaderTitle(bindingAdapterPosition.toString())
                menu.forEach { item ->
                    item.setOnMenuItemClickListener {
                        onContextItemSelected(item, bindingAdapterPosition, rule.first!!)
                    }
                }
            }
        }
        binding.frame2.setOnCreateContextMenuListener { menu, _, _ ->
            if (rule.second != null) {
                fragment.requireActivity().menuInflater.inflate(
                    R.menu.mount_rules_editor_items, menu
                )
                val bindingAdapterPosition = holder.bindingAdapterPosition
                menu.setHeaderTitle(bindingAdapterPosition.toString())
                menu.forEach { item ->
                    item.setOnMenuItemClickListener {
                        onContextItemSelected(item, bindingAdapterPosition, rule.second!!)
                    }
                }
            }
        }
    }

    private fun onContextItemSelected(item: MenuItem, position: Int, path: String): Boolean =
        when (item.itemId) {
            R.id.menu_open -> {
                OpenUtils.open(fragment.requireContext(), path, true)
                true
            }

            R.id.menu_copy -> {
                ClipboardUtils.put(fragment.requireContext(), path)
                Snackbar.make(
                    fragment.requireView(), fragment.getString(R.string.copied, path),
                    Snackbar.LENGTH_SHORT
                ).show()
                true
            }

            R.id.menu_delete -> {
                if (position < itemCount - 1) {
                    answers.updateMountRules {
                        removeAt(position)
                    }
                } else {
                    answers.updateMountRules {
                        set(position, null to null)
                    }
                }
                true
            }

            else -> {
                false
            }
        }

    class ViewHolder(val binding: StorageRedirectCategoryMountRuleItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<Pair<String?, String?>>() {
            override fun areItemsTheSame(
                oldItem: Pair<String?, String?>, newItem: Pair<String?, String?>
            ): Boolean = oldItem.first == newItem.first || oldItem.second == newItem.second

            override fun areContentsTheSame(
                oldItem: Pair<String?, String?>, newItem: Pair<String?, String?>
            ): Boolean = oldItem == newItem
        }
    }
}

class PathsAdapter(
    private val fragment: Fragment, private val answers: WizardAnswers,
    @FilePickerDialog.Companion.SelectType private val selectType: Int
) : BaseKtListAdapter<String?, PathsAdapter.ViewHolder>(CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryMountWizardQuestionsAccessibleFoldersItemBinding.inflate(
            LayoutInflater.from(parent.context)
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val dir = getItem(position)
        binding.text.text = dir
        binding.text.isVisible = dir != null
        binding.add.isVisible = dir == null
        binding.frame.setOnClickListener {
            FilePickerDialog()
                .apply {
                    if (dir != null) {
                        setPath(dir)
                    }
                    setSelectType(selectType)
                    addOnPositiveButtonClickListener { dir ->
                        val position = holder.bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            when (selectType) {
                                SELECT_FOLDER -> answers.updateAccessiblePlaces {
                                    set(position, dir)
                                    if (position == itemCount - 1) {
                                        add(null)
                                    }
                                }

                                SELECT_FILE_AND_FOLDER -> answers.updateInaccessiblePlaces {
                                    set(position, dir)
                                    if (position == itemCount - 1) {
                                        add(null)
                                    }
                                }
                            }
                        }
                    }
                }
                .show(fragment.childFragmentManager, null)
        }
        binding.frame.setOnCreateContextMenuListener { menu, _, _ ->
            if (dir != null) {
                fragment.requireActivity().menuInflater.inflate(
                    R.menu.mount_rules_editor_items, menu
                )
                val bindingAdapterPosition = holder.bindingAdapterPosition
                menu.setHeaderTitle(bindingAdapterPosition.toString())
                menu.forEach { item ->
                    item.setOnMenuItemClickListener {
                        onContextItemSelected(item, bindingAdapterPosition, dir)
                    }
                }
            }
        }
    }

    private fun onContextItemSelected(item: MenuItem, position: Int, path: String): Boolean =
        when (item.itemId) {
            R.id.menu_open -> {
                OpenUtils.open(fragment.requireContext(), path, true)
                true
            }

            R.id.menu_copy -> {
                ClipboardUtils.put(fragment.requireContext(), path)
                Snackbar.make(
                    fragment.requireView(), fragment.getString(R.string.copied, path),
                    Snackbar.LENGTH_SHORT
                ).show()
                true
            }

            R.id.menu_delete -> {
                when (selectType) {
                    SELECT_FOLDER -> answers.updateAccessiblePlaces {
                        removeAt(position)
                    }

                    SELECT_FILE_AND_FOLDER -> answers.updateInaccessiblePlaces {
                        removeAt(position)
                    }
                }
                true
            }

            else -> {
                false
            }
        }

    class ViewHolder(val binding: StorageRedirectCategoryMountWizardQuestionsAccessibleFoldersItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<String?>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }
    }
}
