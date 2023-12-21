package me.gm.cleaner.client.ui

import android.app.Dialog
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.R
import me.gm.cleaner.app.ConfirmationDialog
import me.gm.cleaner.client.CleanerClient
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.databinding.MtrlAlertSelectDialogItemBinding
import me.gm.cleaner.server.observer.FileSystemObserver
import me.gm.cleaner.server.observer.PruneMethod.Companion.DELETE_ALL
import me.gm.cleaner.server.observer.PruneMethod.Companion.DELETE_APP_SPECIFIC
import me.gm.cleaner.server.observer.PruneMethod.Companion.DISTINCT
import me.gm.cleaner.server.observer.PruneMethod.Companion.QUERIED
import me.gm.cleaner.server.observer.PruneMethod.Companion.UNINSTALLED

class PruneRecordsDialog : AppCompatDialogFragment() {
    private val parentViewModel: FileSystemRecordViewModel by viewModels({ requireParentFragment() })
    private val count: Int
        get() = CleanerClient.service!!.databaseCount()
    private val length: Long
        get() = requireContext().createDeviceProtectedStorageContext()
            .getDatabasePath(FileSystemObserver.DATABASE_NAME).length()
    private val databaseSizeDescription: String
        get() = getString(
            R.string.filesystem_record_database_size, count,
            Formatter.formatFileSize(requireContext(), length)
        )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val itemToAction = mutableListOf(
            // 0
            getString(R.string.filesystem_record_clear) to {
                if (PurchaseVerification.isExpressPro) {
                    ConfirmationDialog
                        .newInstance(getString(R.string.filesystem_record_clear))
                        .apply {
                            addOnPositiveButtonClickListener {
                                it.lifecycleScope.launch(Dispatchers.IO) {
                                    parentViewModel.setLoading()
                                    CleanerClient.service!!.pruneRecords(
                                        DELETE_ALL.toLong(), null, false, null
                                    )
                                    parentViewModel.reload()
                                }
                            }
                        }
                        .show(parentFragmentManager, null)
                } else {
                    parentViewModel.setLoading()
                    CleanerClient.service!!.pruneRecords(DELETE_ALL.toLong(), null, false, null)
                    parentViewModel.reload()
                }
            }
        )
        if (PurchaseVerification.isExpressPro) {
            // 1
            itemToAction += getString(R.string.filesystem_record_app_specific_storage) to {
                CleanerClient.service!!.pruneRecords(
                    DELETE_APP_SPECIFIC.toLong(), null, false, null
                )
            }
            // 2
            itemToAction += getString(R.string.filesystem_record_uninstalled_apps) to {
                CleanerClient.service!!.pruneRecords(UNINSTALLED.toLong(), null, false, null)
            }
            // 3
            itemToAction += getString(R.string.filesystem_record_distinct) to {
                CleanerClient.service!!.pruneRecords(DISTINCT.toLong(), null, false, null)
            }
            // 4
            itemToAction += getString(R.string.filesystem_record_queried) to {
                ConfirmationDialog
                    .newInstance(getString(R.string.filesystem_record_queried))
                    .apply {
                        addOnPositiveButtonClickListener {
                            it.lifecycleScope.launch(Dispatchers.IO) {
                                parentViewModel.setLoading()
                                CleanerClient.service!!.pruneRecords(
                                    QUERIED.toLong(), null,
                                    parentViewModel.isHideAppSpecificStorage,
                                    if (parentViewModel.isSearching) parentViewModel.queryText else null
                                )
                                parentViewModel.reload()
                            }
                        }
                    }
                    .show(parentFragmentManager, null)
            }
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filesystem_record_prune_records)
            .setItems(itemToAction.unzip().first.toTypedArray(), null)
            .create()

        val listView = dialog.listView
        var header = refreshDescription(null, listView)
        listView.setOnItemClickListener { _, _, position, _ ->
            when (val action = position - listView.headerViewsCount) {
                0, 4 -> {
                    parentViewModel
                    dismiss()
                    itemToAction[action].second()
                }

                else -> lifecycleScope.launch(Dispatchers.IO) {
                    parentViewModel.setLoading()
                    itemToAction[action].second()
                    parentViewModel.reload()
                    withContext(Dispatchers.Main.immediate) {
                        header = refreshDescription(header, listView)
                    }
                }
            }
        }
        return dialog
    }

    private fun refreshDescription(oldHeader: View?, listView: ListView): View {
        listView.removeHeaderView(oldHeader)
        val binding = MtrlAlertSelectDialogItemBinding.inflate(
            LayoutInflater.from(requireContext()), listView, false
        )
        val titleView = binding.text1
        titleView.text = databaseSizeDescription
        listView.addHeaderView(binding.root, null, false)
        return binding.root
    }
}
