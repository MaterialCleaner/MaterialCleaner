package me.gm.cleaner.app

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import me.gm.cleaner.R
import java.lang.ref.WeakReference

abstract class BaseFragment : Fragment() {
    private lateinit var _appBarLayout: WeakReference<AppBarLayout>
    private lateinit var _toolbar: WeakReference<MaterialToolbar>
    fun requireAppBarLayout(): AppBarLayout = _appBarLayout.get()!!
    fun requireToolbar(): MaterialToolbar = _toolbar.get()!!

    protected fun setAppBar(root: ViewGroup): MaterialToolbar {
        _appBarLayout = WeakReference(root.findViewById(R.id.toolbar_container))
        _toolbar = WeakReference(root.findViewById(R.id.toolbar))
        (requireActivity() as BaseActivity).setSupportActionBar(requireToolbar())
        return requireToolbar()
    }
}
