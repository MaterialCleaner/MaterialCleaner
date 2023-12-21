package me.gm.cleaner.client.ui.storageredirect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.databinding.StorageRedirectCategoryStatusBinding
import me.gm.cleaner.util.DividerViewHolder

class StatusCategoryAdapter(private val runningStatus: String) :
    RecyclerView.Adapter<StatusCategoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryStatusBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.summary.text = runningStatus
    }

    override fun getItemCount(): Int = 1

    class ViewHolder(val binding: StorageRedirectCategoryStatusBinding) :
        DividerViewHolder(binding.root) {
        init {
            isDividerAllowedBelow = true
        }
    }
}
