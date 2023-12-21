package me.gm.cleaner.client.ui.storageredirect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.R
import me.gm.cleaner.client.ui.storageredirect.ConnectionState.Companion.LEVEL_ERROR
import me.gm.cleaner.client.ui.storageredirect.ConnectionState.Companion.LEVEL_INFO
import me.gm.cleaner.client.ui.storageredirect.ConnectionState.Companion.LEVEL_WARN
import me.gm.cleaner.databinding.StorageRedirectCategoryHintBinding
import me.gm.cleaner.util.colorError

class HintCategoryAdapter :
    BaseKtListAdapter<Pair<Int, String>, HintCategoryAdapter.ViewHolder>(CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        StorageRedirectCategoryHintBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val hint = getItem(position)
        val level = hint.first
        require(level in LEVEL_INFO..LEVEL_ERROR)
        val context = binding.root.context
        when (level) {
            LEVEL_INFO -> binding.icon.setImageResource(R.drawable.ic_baseline_info_24)
            LEVEL_WARN -> {
                val color = context.getColor(R.color.color_warning)
                binding.icon.setColorFilter(color)
                binding.title.setTextColor(color)
            }
            LEVEL_ERROR -> {
                val color = context.colorError
                binding.icon.setColorFilter(color)
                binding.title.setTextColor(color)
            }
        }
        binding.title.text = hint.second
    }

    class ViewHolder(val binding: StorageRedirectCategoryHintBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<Pair<Int, String>>() {
            override fun areItemsTheSame(
                oldItem: Pair<Int, String>, newItem: Pair<Int, String>
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: Pair<Int, String>, newItem: Pair<Int, String>
            ): Boolean = oldItem == newItem
        }
    }
}

@Retention(AnnotationRetention.SOURCE)
annotation class ConnectionState {
    companion object {
        const val LEVEL_INFO: Int = 0
        const val LEVEL_WARN: Int = 1
        const val LEVEL_ERROR: Int = 2
    }
}
