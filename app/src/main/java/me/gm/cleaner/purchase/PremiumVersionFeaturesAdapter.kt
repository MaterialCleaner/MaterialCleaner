package me.gm.cleaner.purchase

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.R
import me.gm.cleaner.databinding.PurchaseFreewheelingBinding
import me.gm.cleaner.databinding.PurchaseHandyBinding
import me.gm.cleaner.databinding.PurchasePithyBinding
import me.gm.cleaner.util.DividerViewHolder

class PremiumVersionFeaturesAdapter : RecyclerView.Adapter<DividerViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (position) {
        0 -> R.layout.purchase_freewheeling
        1 -> R.layout.purchase_handy
        2 -> R.layout.purchase_pithy
        else -> throw IndexOutOfBoundsException()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DividerViewHolder =
        when (viewType) {
            R.layout.purchase_freewheeling -> FreewheelingViewHolder(
                PurchaseFreewheelingBinding.inflate(LayoutInflater.from(parent.context))
            )
            R.layout.purchase_handy -> HandyViewHolder(
                PurchaseHandyBinding.inflate(LayoutInflater.from(parent.context))
            )
            R.layout.purchase_pithy -> PithyButtonViewHolder(
                PurchasePithyBinding.inflate(LayoutInflater.from(parent.context))
            )
            else -> throw IndexOutOfBoundsException()
        }

    override fun onBindViewHolder(holder: DividerViewHolder, position: Int) {}

    override fun getItemCount(): Int = 3

    class FreewheelingViewHolder(val binding: PurchaseFreewheelingBinding) :
        DividerViewHolder(binding.root) {
        init {
            isDividerAllowedBelow = true
        }
    }

    class HandyViewHolder(val binding: PurchaseHandyBinding) : DividerViewHolder(binding.root) {
        init {
            isDividerAllowedBelow = true
        }
    }

    class PithyButtonViewHolder(val binding: PurchasePithyBinding) : DividerViewHolder(binding.root)
}
