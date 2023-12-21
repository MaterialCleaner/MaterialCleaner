package me.gm.cleaner.client.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import me.gm.cleaner.R
import me.gm.cleaner.app.BaseActivity
import me.gm.cleaner.app.BaseFragment
import me.gm.cleaner.dao.PurchaseVerification
import me.gm.cleaner.databinding.ServiceSettingsFragmentBinding
import java.lang.ref.WeakReference

class ServiceSettingsFragment : BaseFragment() {
    val viewPagerItems: List<Class<out BaseServiceSettingsFragment>> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= 35 && !PurchaseVerification.isExpressPro) {
            AppListUnsupportedFragment::class.java
        } else {
            AppListFragment::class.java
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            FileSystemRecordFragment::class.java
        } else {
            null
        },
        MoreOptionsFragmentStub::class.java,
    )
    private lateinit var viewPagerRef: WeakReference<ViewPager2>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ServiceSettingsFragmentBinding.inflate(layoutInflater)
        val toolbar = setAppBar(binding.root)
        if (!PurchaseVerification.isExpressPro) {
            toolbar.setSubtitle(R.string.free_version)
        }
        (requireActivity() as BaseActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val viewPager = binding.viewPager
        viewPagerRef = WeakReference(viewPager)
        viewPager.adapter = object :
            FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {

            override fun createFragment(position: Int): Fragment =
                viewPagerItems[position].getDeclaredConstructor().newInstance()

            override fun getItemCount(): Int = viewPagerItems.size
        }
        TabLayoutMediator(binding.root.findViewById(R.id.tabs), viewPager) { tab, position ->
            tab.text = getString(
                when (viewPagerItems[position]) {
                    AppListFragment::class.java, AppListUnsupportedFragment::class.java -> R.string.storage_redirect_title
                    FileSystemRecordFragment::class.java -> R.string.filesystem_record_title
                    MoreOptionsFragmentStub::class.java -> androidx.appcompat.R.string.abc_action_menu_overflow_description
                    else -> throw IndexOutOfBoundsException()
                }
            ).uppercase()
        }.attach()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // https://stackoverflow.com/questions/62851425/viewpager2-inside-a-fragment-leaks-after-replacing-the-fragment-its-in-by-navig
        viewPagerRef.get()?.adapter = null
        viewPagerRef.clear()
    }
}
