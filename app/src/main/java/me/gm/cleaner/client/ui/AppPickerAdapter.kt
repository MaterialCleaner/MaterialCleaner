package me.gm.cleaner.client.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import me.gm.cleaner.databinding.AppPickerItemBinding

class AppPickerAdapter(private val viewModel: AppPickerViewModel) :
    BaseKtListAdapter<AppPickerModel, AppPickerAdapter.ViewHolder>(CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        AppPickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val model = getItem(position)
        binding.iconImage.load(model.packageInfo)
        binding.principalText.text = model.label
        binding.labelText.text = model.packageInfo.packageName
        binding.checkbox.isChecked = model.isChecked
        binding.root.setOnClickListener {
            viewModel.toggle(model.packageInfo)
        }
    }

    class ViewHolder(val binding: AppPickerItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<AppPickerModel>() {
            override fun areItemsTheSame(
                oldItem: AppPickerModel, newItem: AppPickerModel
            ): Boolean = oldItem.packageInfo.packageName == newItem.packageInfo.packageName

            override fun areContentsTheSame(
                oldItem: AppPickerModel, newItem: AppPickerModel
            ): Boolean = oldItem == newItem
        }
    }
}
