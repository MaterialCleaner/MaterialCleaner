package me.gm.cleaner.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.gm.cleaner.R
import me.gm.cleaner.databinding.PremiumVersionFeaturesFragmentBinding
import me.gm.cleaner.util.DividerDecoration
import me.gm.cleaner.util.fixEdgeEffect
import me.gm.cleaner.util.overScrollIfContentScrollsPersistent

class PremiumVersionFeaturesFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = PremiumVersionFeaturesFragmentBinding.inflate(layoutInflater)

        val list = binding.list
        list.adapter = PremiumVersionFeaturesAdapter()
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect()
        list.overScrollIfContentScrollsPersistent()
        list.addItemDecoration(DividerDecoration(list).apply {
            setDivider(resources.getDrawable(R.drawable.list_divider_material, null))
            setAllowDividerAfterLastItem(false)
        })

        return binding.root
    }
}
