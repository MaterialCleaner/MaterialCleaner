package me.gm.cleaner.client.ui.storageredirect

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import me.gm.cleaner.R
import me.gm.cleaner.app.filepicker.FilePickerDialog
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FILE_AND_FOLDER
import me.gm.cleaner.app.filepicker.FilePickerDialog.Companion.SelectType.Companion.SELECT_FOLDER
import me.gm.cleaner.databinding.StorageRedirectCategoryMountButtonsBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryMountHeaderBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryMountRuleHeaderBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryMountRuleItemBinding
import me.gm.cleaner.databinding.StorageRedirectCategoryMountWizardQuestionsBinding
import me.gm.cleaner.util.ClipboardUtils
import me.gm.cleaner.util.DividerViewHolder
import me.gm.cleaner.util.OpenUtils

class MountHeaderAdapter : RecyclerView.Adapter<MountHeaderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryMountHeaderBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: StorageRedirectCategoryMountHeaderBinding) :
        DividerViewHolder(binding.root) {
        init {
            isDividerAllowedAbove = true
        }
    }
}

class MountRuleTitleAdapter : RecyclerView.Adapter<MountRuleTitleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryMountRuleHeaderBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: StorageRedirectCategoryMountRuleHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)
}

class MountRulesAdapter(
    private val fragment: StorageRedirectFragment, private val viewModel: StorageRedirectViewModel
) : BaseKtListAdapter<Pair<String?, String?>, MountRulesAdapter.ViewHolder>(CALLBACK) {
    private var meaninglessRulesIndices: Set<Int> = emptySet()

    override fun onCurrentListChanged(
        previousList: List<Pair<String?, String?>>, currentList: List<Pair<String?, String?>>
    ) {
        val previous = meaninglessRulesIndices
        val current = viewModel.rules.meaninglessRulesIndices.toSet()
        val meaninglessRuleInCurrentList = current.asSequence().map { currentList[it] }.toSet()
        previousList.forEachIndexed { position, rule ->
            if (previous.contains(position) || meaninglessRuleInCurrentList.contains(rule)) {
                notifyItemChanged(position)
            }
        }
        meaninglessRulesIndices = current
    }

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
        val isMeaningless = meaninglessRulesIndices.contains(position)
        binding.text1.isEnabled = !isMeaningless
        binding.text2.isEnabled = !isMeaningless
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
                            viewModel.updateMountRules {
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
                            viewModel.updateMountRules {
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
                    viewModel.updateMountRules {
                        removeAt(position)
                    }
                } else {
                    viewModel.updateMountRules {
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

class WizardAdapter(
    private val fragment: StorageRedirectFragment, private val viewModel: StorageRedirectViewModel
) : RecyclerView.Adapter<WizardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryMountWizardQuestionsBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        viewModel.wizard.bindView(viewModel.mountRules, holder.binding, fragment)
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: StorageRedirectCategoryMountWizardQuestionsBinding) :
        RecyclerView.ViewHolder(binding.root)
}

class ButtonsAdapter(
    private val fragment: StorageRedirectFragment, private val viewModel: StorageRedirectViewModel
) : RecyclerView.Adapter<ButtonsAdapter.ViewHolder>() {
    private val testAdapter: TestCategoryAdapter = TestCategoryAdapter(fragment, viewModel)

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        testAdapter.onViewAttachedToWindow(holder.binding.welcome.result)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryMountButtonsBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        when (viewModel.mode) {
            is Mode.Welcome -> {
                binding.welcome.root.isVisible = true
                binding.questions.root.isVisible = false
                binding.welcome.title.isVisible = true
                binding.welcome.manual.isVisible = true
                binding.welcome.test.isVisible = false
                binding.welcome.result.root.isVisible = false
                binding.welcome.followWizard.setOnClickListener {
                    viewModel.mode = Mode.Wizard
                }
                binding.welcome.manual.setOnClickListener {
                    viewModel.mode = Mode.Editor
                }
            }

            is Mode.Editor -> {
                binding.welcome.root.isVisible = true
                binding.questions.root.isVisible = false
                binding.welcome.title.isVisible = false
                binding.welcome.manual.isVisible = false
                binding.welcome.test.isVisible = viewModel.test.isEmpty()
                binding.welcome.result.root.isVisible = viewModel.test.isNotEmpty()
                testAdapter.onBindViewHolder(binding.welcome.result)
                binding.welcome.followWizard.setOnClickListener {
                    viewModel.mode = Mode.Wizard
                }
                binding.welcome.test.setOnClickListener {
                    FilePickerDialog()
                        .apply {
                            setSelectType(SELECT_FILE_AND_FOLDER)
                            addOnPositiveButtonClickListener { dir ->
                                viewModel.test = dir
                            }
                        }
                        .show(fragment.childFragmentManager, null)
                }
            }

            is Mode.Wizard -> {
                binding.welcome.root.isVisible = false
                binding.questions.root.isVisible = true
                viewModel.wizard.bindButton(binding.questions, fragment) {
                    viewModel.appTypeMarks.getOrNull()
                }
                binding.questions.submit.setOnClickListener {
                    viewModel.updateMountRules {
                        clear()
                        addAll(viewModel.wizard.createRules())
                        add(null to null)
                    }
                    viewModel.mode = Mode.Editor
                }
                binding.questions.up.setOnClickListener {
                    viewModel.mode = if (viewModel.mountRules.isNotEmpty()) {
                        Mode.Editor
                    } else {
                        Mode.Welcome
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: StorageRedirectCategoryMountButtonsBinding) :
        RecyclerView.ViewHolder(binding.root)
}
