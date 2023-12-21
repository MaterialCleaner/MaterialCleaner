package me.gm.cleaner.home.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.transition.TransitionInflater
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.SharedElementCallback
import androidx.core.os.postDelayed
import androidx.core.view.postOnAnimationDelayed
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import me.gm.cleaner.R
import me.gm.cleaner.app.BaseFragment
import me.gm.cleaner.app.ConfirmationDialog
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.databinding.TrashFragmentBinding
import me.gm.cleaner.home.scanner.ScannerManager
import me.gm.cleaner.home.scanner.ScannerViewModel
import me.gm.cleaner.util.*
import me.gm.cleaner.widget.recyclerview.AlphaAwareItemAnimator
import me.gm.cleaner.widget.recyclerview.fastscroll.LinearLayoutViewHelper
import me.gm.cleaner.widget.recyclerview.fastscroll.useThemeStyle
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.util.function.Predicate

class TrashFragment : BaseFragment() {
    private val args: TrashFragmentArgs by navArgs()
    private val viewModel: TrashViewModel by viewModels()
    private val scannerViewModel: ScannerViewModel by lazy {
        ViewModelProvider(requireActivity())[args.viewModelClass as Class<out ScannerViewModel>]
    }
    private lateinit var adapter: TrashAdapter
    private lateinit var list: RecyclerView
    private val isShScanner: Boolean by lazy { args.serviceClass as Class<*>? == null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = TrashFragmentBinding.inflate(inflater)
        setAppBar(binding.root)
        val toolbar = requireToolbar()
        toolbar.apply {
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
            toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24)
        }

        adapter = TrashAdapter(this).apply {
            setHasStableIds(true)
        }
        list = binding.list
        requireAppBarLayout().setLiftOnScrollTargetView(list)
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        val fastScroll = FastScrollerBuilder(list)
            .setViewHelper(LinearLayoutViewHelper(list))
            .useThemeStyle(requireContext())
            .build()
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        list.itemAnimator = object : AlphaAwareItemAnimator() {
            override val isTranslucent = Predicate<RecyclerView.ViewHolder> { holder ->
                !(holder as TrashAdapter.ViewHolder).binding.root.isChecked
            }

            override fun onAnimationFinished(viewHolder: RecyclerView.ViewHolder) {
                super.onAnimationFinished(viewHolder)
                list.overScrollIfContentScrolls()
            }
        }
        list.fitsSystemWindowInsets(fastScroll)
        binding.hint.fitsSystemWindowInsets()
        binding.fab.setOnClickListener {
            if (RootPreferences.isConfirmDelete) {
                ConfirmationDialog
                    .newInstance(getString(R.string.confirm_delete))
                    .apply {
                        addOnPositiveButtonClickListener {
                            val parentFragment = it.requireParentFragment() as TrashFragment
                            parentFragment.deleteTrashes()
                        }
                    }
                    .show(childFragmentManager, null)
            } else {
                deleteTrashes()
            }
        }

        val trashesFlow = viewModel.trashesFlow(
            scannerViewModel.trashesLiveData.asFlow(), scannerViewModel.progressFlow
        )
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                trashesFlow.collect {
                    val trashes = when (it) {
                        is TrashState.Scanning -> it.list
                        is TrashState.Done -> it.list
                        is TrashState.Cleaning -> it.list
                        is TrashState.Cleared -> it.list
                    }
                    adapter.submitList(trashes)

                    var checkedCount = 0
                    var length = 0L
                    for (trash in trashes) {
                        if (trash.isChecked) {
                            checkedCount++
                            length += trash.length
                        }
                    }
                    if (it !is TrashState.Cleared) {
                        toolbar.title = getString(
                            R.string.trash_toolbar_title, trashes.size, getString(args.title)
                        )
                        toolbar.subtitle = getString(
                            R.string.trash_toolbar_subtitle_prefix, checkedCount
                        ) + if (RootPreferences.isShowLength) getString(
                            R.string.trash_toolbar_subtitle_suffix,
                            Formatter.formatFileSize(requireContext(), length)
                        ) else ""
                    }

                    when (it) {
                        is TrashState.Scanning -> binding.progress.progress = it.progress
                        is TrashState.Done -> {
                            binding.progress.postOnAnimationDelayed(requireContext().mediumAnimTime) {
                                binding.progress.hide()
                            }
                            if (viewModel.isSearching) {
                                binding.fab.hide()
                            } else {
                                binding.fab.show()
                            }
                        }

                        is TrashState.Cleaning -> {
                            binding.fab.isClickable = false
                            binding.fab.hide()
                            toolbar.setTitle(R.string.trash_toolbar_title_cleaning)
                        }

                        is TrashState.Cleared -> {
                            binding.fab.isClickable = false
                            binding.fab.hide()
                            toolbar.setTitle(args.title)
                            toolbar.subtitle = null
                            val itemAnimator = list.itemAnimator as AlphaAwareItemAnimator
                            Handler(Looper.getMainLooper()).postDelayed(itemAnimator.removeDuration) {
                                itemAnimator.isRunning {
                                    findNavController().navigateUp()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (savedInstanceState != null && scannerViewModel.progress == -2) {
            ScannerManager.staticScanners.first { it.viewModelClass == args.viewModelClass }
                .newInstance().start()
        }

        prepareSharedElementTransition(binding.progress)
        super.onCreateView(inflater, container, savedInstanceState)
        return binding.root
    }

    private fun deleteTrashes() {
        //noinspection notifyDataSetChanged
        adapter.notifyDataSetChanged()
        list.itemAnimator = object : AlphaAwareItemAnimator() {
            override fun onAnimationFinished(viewHolder: RecyclerView.ViewHolder) {
                super.onAnimationFinished(viewHolder)
                list.overScrollIfContentScrolls()
            }

            override fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
                val view = holder.itemView
                val animation = view.animate()
                mRemoveAnimations.add(holder)
                animation.setDuration(removeDuration).translationX(view.width.toFloat())
                    .setListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animator: Animator) {
                                dispatchRemoveStarting(holder)
                            }

                            override fun onAnimationEnd(animator: Animator) {
                                animation.setListener(null)
                                view.translationX = 0F
                                dispatchRemoveFinished(holder)
                                mRemoveAnimations.remove(holder)
                                dispatchFinishedWhenDone()
                            }
                        }).start()
            }
        }
        viewModel.deleteTrashes()
    }

    @SuppressLint("RestrictedApi")
    private fun prepareSharedElementTransition(progress: LinearProgressIndicator) {
        val toolbarTitle = Toolbar::class.java.getDeclaredMethod("getTitleTextView")
            .apply { isAccessible = true }
            .invoke(requireToolbar()) as TextView
        toolbarTitle.transitionName =
            "mTitleTextView${ScannerManager.lastRegisteredScannerPosition}"
        progress.transitionName = getString(args.title)

        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.quick_move)

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                sharedElements[names[0]] = toolbarTitle
                sharedElements[names[1]] = progress
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.trash_toolbar, menu)
        val searchItem = menu.findItem(R.id.menu_search)
        if (viewModel.isSearching) {
            searchItem.expandActionView()
        }
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                viewModel.isSearching = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.isSearching = false
                return true
            }
        })
        val searchView = searchItem.actionView as SearchView
        searchView.setQuery(viewModel.queryText, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.queryText = query
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.queryText = newText
                return false
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (isShScanner &&
            RootPreferences.isMonitor && ScannerManager.runningShScanners.isEmpty() &&
            PermissionUtils.checkSelfStoragePermissions(requireContext()) &&
            ScannerManager.isFileChanged
        ) {
            Snackbar
                .make(
                    requireView().findViewById(R.id.fab),
                    R.string.recommend_rescan,
                    Snackbar.LENGTH_SHORT
                )
                .setAction(R.string.rescan) {
                    ScannerManager.shScannersRescan()
                }
                .show()
        }
    }
}
