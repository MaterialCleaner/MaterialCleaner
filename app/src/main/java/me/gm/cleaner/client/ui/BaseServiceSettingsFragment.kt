package me.gm.cleaner.client.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import me.gm.cleaner.R

abstract class BaseServiceSettingsFragment : Fragment() {
    abstract val viewModel: BaseServiceSettingsViewModel
    protected val viewPager: ViewPager2
        get() = requireParentFragment().requireView().findViewById(R.id.view_pager)
    protected val viewPagerItems: List<Class<out BaseServiceSettingsFragment>>
        get() = (requireParentFragment() as ServiceSettingsFragment).viewPagerItems

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(true)
        val appBarLayout = (requireParentFragment() as ServiceSettingsFragment)
            .requireAppBarLayout()
        val list = requireView().findViewById<RecyclerView>(R.id.list) ?: return
        appBarLayout.setLiftOnScrollTargetView(list)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
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
                if (viewPagerItems[viewPager.currentItem] ==
                    this@BaseServiceSettingsFragment.javaClass
                ) {
                    viewModel.queryText = newText
                }
                return false
            }
        })
    }
}
