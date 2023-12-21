package me.gm.cleaner.home.scanner.sh

import android.Manifest
import android.os.Build
import android.os.Bundle
import me.gm.cleaner.R
import me.gm.cleaner.app.ConfirmationDialog
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.home.StaticScanner
import me.gm.cleaner.home.TrashModel
import me.gm.cleaner.home.scanner.BaseScanner
import me.gm.cleaner.home.scanner.ScannerFileVisitor
import me.gm.cleaner.home.scanner.ScannerManager.FileChanges
import me.gm.cleaner.home.scanner.ScannerPredicate
import me.gm.cleaner.nio.file.walkPathTreeProgressed
import me.gm.cleaner.util.FileUtils
import me.gm.cleaner.util.PermissionUtils
import me.gm.cleaner.util.RequesterFragment
import java.io.File
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.math.roundToInt

abstract class BaseShScanner protected constructor(info: StaticScanner) : BaseScanner(info),
    ScannerPredicate {
    protected val externalStorageDir: File = FileUtils.externalStorageDir
    protected var changedPaths: List<String> = emptyList()

    class ShRequesterFragment : RequesterFragment() {
        override val requiredPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        override fun onRequestPermissionsFailure(
            shouldShowRationale: Set<String>, permanentlyDenied: Set<String>,
            haveAskedUser: Boolean, savedInstanceState: Bundle?
        ) {
            if (shouldShowRationale.isNotEmpty()) {
                ConfirmationDialog
                    .newInstance(
                        getString(
                            R.string.rationale_shouldShowRationale,
                            getString(R.string.rationale_scanning_files)
                        )
                    )
                    .apply {
                        addOnPositiveButtonClickListener {
                            onRequestPermissions(
                                shouldShowRationale.toTypedArray(), savedInstanceState
                            )
                        }
                    }
                    .show(childFragmentManager, null)
            } else if (permanentlyDenied.isNotEmpty()) {
                ConfirmationDialog
                    .newInstance(
                        getString(
                            R.string.rationale_permanentlyDenied,
                            getString(R.string.rationale_scanning_files)
                        )
                    )
                    .apply {
                        addOnPositiveButtonClickListener {
                            PermissionUtils.startDetailsSettings(it.requireContext())
                        }
                    }
                    .show(childFragmentManager, null)
            }
        }
    }

    override fun onCheckPermission(): Boolean =
        PermissionUtils.checkSelfStoragePermissions(activity)

    override fun onRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ConfirmationDialog
                .newInstance(
                    activity.getString(
                        R.string.rationale_external_storage_manager,
                        activity.getString(R.string.rationale_scanning_files)
                    )
                )
                .apply {
                    addOnPositiveButtonClickListener {
                        PermissionUtils.startManageAllFilesAccessPermission(it.requireContext())
                    }
                }
                .show(activity.supportFragmentManager, null)
        } else {
            PermissionUtils.requestPermissions(
                activity.supportFragmentManager, ShRequesterFragment()
            )
        }
    }

    open fun acceptMonitor(fileChanges: FileChanges): BaseShScanner {
        changedPaths = fileChanges.changedPaths
        return this
    }

    /**
     * This is an expensive helper function,
     * so the input list should have as few elements as possible.
     */
    protected fun distinctChangedPaths(changedPaths: Iterable<Path>): MutableList<Path> {
        val scanPaths = mutableListOf<Path>()
        for (new in changedPaths) {
            run loop@{
                scanPaths.forEachIndexed { index, existing ->
                    when {
                        new.startsWith(existing) -> {
                            return@loop
                        }

                        existing.startsWith(new) -> {
                            // Replace existing with new.
                            scanPaths[index] = new
                            return@loop
                        }
                    }
                }
                scanPaths.add(new)
            }
        }
        return scanPaths
    }

    protected fun toRootDir(path: String): String {
        val root = externalStorageDir.path + File.separator
        return root + path.substring(root.length).substringBefore(File.separator)
    }

    protected open fun removeChangedInheritance(scanPaths: List<Path>) {
        viewModel.removeTrashIf {
            val child = it.path
            for (parent in scanPaths) {
                if (FileUtils.startsWith(parent, child)) {
                    return@removeTrashIf true
                }
            }
            false
        }
    }

    protected open fun initScanPaths(): List<Path> {
        val scanPaths = mutableListOf<Path>()
        if (viewModel.isAcceptInheritance) {
            val partition = changedPaths
                .map { Path(it) }
                .partition { it.exists(LinkOption.NOFOLLOW_LINKS) }
            scanPaths += distinctChangedPaths(partition.first)
            removeChangedInheritance(partition.second)
            removeChangedInheritance(scanPaths)
        } else {
            viewModel.removeTrashIf { true }
            scanPaths.add(externalStorageDir.toPath())
        }
        return scanPaths
    }

    public override suspend fun onScan() {
        val scanPaths = initScanPaths()
        val noScanPaths = RootPreferences.noScan
            .filter { FileUtils.startsWith(externalStorageDir, it) }
            .toSet() + FileUtils.defaultExternalNoScan.map { it.path }
        scanPaths.forEachIndexed { index, path ->
            if (noScanPaths.any { path.startsWith(it) }) {
                return@forEachIndexed
            }
            path.walkPathTreeProgressed(visitor = object : ScannerFileVisitor(noScanPaths, this) {
                override fun onProgress(progress: Float) {
                    viewModel.progress = (100 * (progress + index) / scanPaths.size)
                        .roundToInt()
                }

                override fun onTrashFound(trashBuilder: TrashModel.Builder) {
                    this@BaseShScanner.onTrashFound(trashBuilder)
                }
            })
        }
    }

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): Boolean = false
    override fun visitFile(file: Path, attrs: BasicFileAttributes): Boolean = false
    override fun postVisitDirectory(dir: Path): Boolean = false

    override fun onTrashFound(trashBuilder: TrashModel.Builder) {
        if (RootPreferences.isShowLength) {
            trashBuilder.length = try {
                FileUtils.fileTreeSize(Path(trashBuilder.path))
            } catch (e: Exception) {
                return
            }
        }
        super.onTrashFound(trashBuilder)
    }
}
