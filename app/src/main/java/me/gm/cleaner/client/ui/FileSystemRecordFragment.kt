package me.gm.cleaner.client.ui

import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceGroup.PreferencePositionCallback
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import androidx.viewpager2.widget.ViewPager2
import me.gm.cleaner.R
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.client.ui.FileSystemRecordViewModel.Companion.LOAD_SIZE
import me.gm.cleaner.dao.ServicePreferences
import me.gm.cleaner.databinding.FilesystemRecordFragmentBinding
import me.gm.cleaner.util.buildStyledTitle
import me.gm.cleaner.util.colorAccent
import me.gm.cleaner.util.fitsSystemWindowInsets
import me.gm.cleaner.util.fixEdgeEffect
import me.gm.cleaner.util.isItemCompletelyVisible
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent
import me.gm.cleaner.widget.recyclerview.fastscroll.LinearLayoutViewHelper
import me.gm.cleaner.widget.recyclerview.fastscroll.useThemeStyle
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class FileSystemRecordFragment : BaseServiceSettingsFragment() {
    override val viewModel: FileSystemRecordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FilesystemRecordFragmentBinding.inflate(layoutInflater)

        val fileSystemRecordAdapter = FileSystemRecordAdapter(this).apply {
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        val loadingMoreAdapter = LoadingMoreAdapter()
        val adapters = ConcatAdapter(fileSystemRecordAdapter)
        val layoutManager = GridLayoutManager(requireContext(), 1)
        val list = binding.list
        list.adapter = adapters
        list.layoutManager = layoutManager
        list.setHasFixedSize(true)
        val fastScroll = FastScrollerBuilder(list)
            .setViewHelper(LinearLayoutViewHelper(list))
            .useThemeStyle(requireContext())
            .build()
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        list.fitsSystemWindowInsets(fastScroll, savedInstanceState == null)

        viewModel.fileSystemRecordLiveData.observe(viewLifecycleOwner) { record ->
            when (record) {
                is FileSystemRecordState.Disabled -> {
                    binding.progress.hide()
                    binding.warningContainer.isVisible = true
                    binding.warning.setText(R.string.filesystem_record_activation_instruction)
                    binding.button.setText(R.string.filesystem_record_enable)
                    binding.button.setOnClickListener {
                        val index = viewPagerItems.indexOf(MoreOptionsFragmentStub::class.java)
                        viewPager.setCurrentItem(index, true)
                        viewPager.postOnAnimation {
                            viewPager.registerOnPageChangeCallback(object :
                                ViewPager2.OnPageChangeCallback() {
                                override fun onPageScrollStateChanged(state: Int) {
                                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                                        highlightPreferences(index)
                                    }
                                    viewPager.unregisterOnPageChangeCallback(this)
                                }
                            })
                        }
                    }
                }

                is FileSystemRecordState.DbTooLarge -> {
                    binding.progress.hide()
                    binding.warningContainer.isVisible = true
                    binding.warning.setText(R.string.filesystem_record_database_too_large)
                    binding.button.setText(R.string.filesystem_record_database_load_anyway)
                    binding.button.setOnClickListener {
                        viewModel.startLoadingRecord()
                    }
                }

                is FileSystemRecordState.Loading -> {
                    binding.progress.show()
                    binding.warningContainer.isVisible = false
                    fileSystemRecordAdapter.submitList(emptyList())
                }

                is FileSystemRecordState.LoadingMore -> {
                    binding.progress.hide()
                    loadingMoreAdapter.setProgress(record.progress)
                    if (adapters.addAdapter(loadingMoreAdapter)) {
                        list.scrollToPosition(adapters.itemCount - 1)
                    }
                }

                is FileSystemRecordState.Done -> {
                    binding.progress.hide()
                    if (record.list.size - fileSystemRecordAdapter.currentList.size > 5 * LOAD_SIZE) {
                        fileSystemRecordAdapter.setCurrentList(record.list) {
                            adapters.removeAdapter(loadingMoreAdapter)
                            //noinspection notifyDataSetChanged
                            fileSystemRecordAdapter.notifyDataSetChanged()
                        }
                    } else {
                        fileSystemRecordAdapter.submitList(record.list) {
                            adapters.removeAdapter(loadingMoreAdapter)
                        }
                    }
                }
            }
        }

        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    list.doOnPreDraw {
                        maybeLoadMoreFileSystemRecord(
                            layoutManager,
                            fileSystemRecordAdapter
                        )
                    }
                }
            }

            override fun onScrolled(list: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    maybeLoadMoreFileSystemRecord(layoutManager, fileSystemRecordAdapter)
                }
            }
        })
        super.onCreateView(inflater, container, savedInstanceState)
        return binding.root
    }

    private fun highlightPreferences(index: Int) {
        val targetFragment = parentFragmentManager.findFragmentByTag("f$index") ?: return
        val listView = (targetFragment.childFragmentManager.fragments[0] as MoreOptionsFragment)
            .listView
        val callback = (listView.adapter as PreferencePositionCallback)
        val preferenceAdapterPositions = arrayOf(
            me.gm.cleaner.shared.R.string.record_shared_storage_key,
            me.gm.cleaner.shared.R.string.record_external_app_specific_storage_key
        )
            .map { keyStringId -> callback.getPreferenceAdapterPosition(getString(keyStringId)) }
            .filter { position -> position != RecyclerView.NO_POSITION }
        val scrollTargetPosition = preferenceAdapterPositions.max()

        fun animate() {
            preferenceAdapterPositions.forEach { position ->
                val holder = listView.findViewHolderForAdapterPosition(position)
                if (holder != null) {
                    val background = holder.itemView.background as RippleDrawable
                    background.state = intArrayOf(
                        android.R.attr.state_pressed, android.R.attr.state_enabled
                    )
                    listView.postDelayed(1000L) {
                        background.state = intArrayOf()
                    }
                }
            }
        }
        if (listView.isItemCompletelyVisible(scrollTargetPosition)) {
            animate()
        } else {
            listView.smoothScrollToPosition(scrollTargetPosition)
            listView.postOnAnimation {
                listView.addOnScrollListener(
                    object : RecyclerView.OnScrollListener() {
                        override fun onScrollStateChanged(
                            recyclerView: RecyclerView, newState: Int
                        ) {
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                animate()
                            }
                            listView.removeOnScrollListener(this)
                        }
                    }
                )
            }
        }
    }

    private fun shouldLoadMoreFileSystemRecord(
        layoutManager: LinearLayoutManager, adapter: RecyclerView.Adapter<*>
    ): Boolean = viewModel.fileSystemRecordLiveData.value is FileSystemRecordState.Done &&
            layoutManager.findLastCompletelyVisibleItemPosition() == adapter.itemCount - 1

    private fun maybeLoadMoreFileSystemRecord(
        layoutManager: LinearLayoutManager, adapter: FileSystemRecordAdapter
    ) {
        if (shouldLoadMoreFileSystemRecord(layoutManager, adapter)) {
            viewModel.loadMore()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.filesystem_record_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.menu_hide_app_specific_storage).isChecked =
            ServicePreferences.isHideAppSpecificStorage
        arrayOf(menu.findItem(R.id.menu_header_hide)).forEach {
            it.title = requireContext().buildStyledTitle(
                it.title!!,
                androidx.appcompat.R.attr.textAppearancePopupMenuHeader,
                requireContext().colorAccent
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_hide_pick_apps -> AppPickerDialog()
                .apply {
                    setAllAppsSupplier { CleanerClient.getInstalledPackages(0) }
                    setSelection(viewModel.checkedFilterApps)
                    addOnPositiveButtonClickListener { checkedApps ->
                        viewModel.checkedFilterApps = checkedApps
                    }
                }
                .show(childFragmentManager, null)

            R.id.menu_hide_app_specific_storage -> {
                val isHideAppSpecificStorage = !item.isChecked
                item.isChecked = isHideAppSpecificStorage
                viewModel.isHideAppSpecificStorage = isHideAppSpecificStorage
            }

            R.id.menu_filesystem_record_prune_records -> PruneRecordsDialog()
                .show(childFragmentManager, null)

            R.id.menu_filesystem_record_load_all -> viewModel.loadAll()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
