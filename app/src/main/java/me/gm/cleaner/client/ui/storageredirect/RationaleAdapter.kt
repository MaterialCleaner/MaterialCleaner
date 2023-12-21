package me.gm.cleaner.client.ui.storageredirect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.BaseKtListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapePath
import me.gm.cleaner.R
import me.gm.cleaner.databinding.RationaleHeaderBinding
import me.gm.cleaner.databinding.RationaleItemBinding
import me.gm.cleaner.util.dpToPx

class RationaleAdapter : BaseKtListAdapter<RationaleModel, RecyclerView.ViewHolder>(CALLBACK) {
    private lateinit var content: List<Pair<String, String>>
    private val collapsedIndex: MutableSet<Int> = mutableSetOf()

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is RationaleModel.Header -> R.layout.rationale_header
        is RationaleModel.Item -> R.layout.rationale_item
        else -> throw IndexOutOfBoundsException()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.rationale_header -> HeaderViewHolder(
                RationaleHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    .apply {
                        val context = root.context
                        val materialShapeDrawable = MaterialAlertDialogBuilder(context)
                            .background as MaterialShapeDrawable
                        materialShapeDrawable.shapeAppearanceModel = materialShapeDrawable
                            .shapeAppearanceModel
                            .toBuilder()
                            .setAllCornerSizes(0F)
                            .setTopEdge(
                                object : EdgeTreatment() {
                                    override fun getEdgePath(
                                        length: Float, center: Float,
                                        interpolation: Float, shapePath: ShapePath
                                    ) {
                                        val dividerThickness = context.dpToPx(1).toFloat()
                                        shapePath.lineTo(0F, dividerThickness)
                                        shapePath.lineTo(length, dividerThickness)
                                    }
                                }
                            )
                            .build()
                        root.background = materialShapeDrawable
                    }
            )

            R.layout.rationale_item -> ItemViewHolder(
                RationaleItemBinding.inflate(LayoutInflater.from(parent.context))
            )

            else -> throw IndexOutOfBoundsException()
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.binding.text.text = getItem(position).text
                val index = content.indexOfFirst { (header, item) ->
                    header == getItem(position).text
                }
                holder.binding.arrow.rotationX = if (index in collapsedIndex) 0F else 180F
                holder.binding.root.setOnClickListener {
                    holder.binding.arrow.rotationX = if (collapsedIndex.add(index)) {
                        0F
                    } else {
                        collapsedIndex.remove(index)
                        180F
                    }
                    notifyListChanged()
                }
            }

            is ItemViewHolder -> {
                holder.binding.root.text = getItem(position).text
            }
        }
    }

    @JvmName("preSubmitList")
    fun submitList(content: List<Pair<String, String>>) {
        this.content = content
        notifyListChanged()
    }

    private fun notifyListChanged() {
        val list = ArrayList<RationaleModel>(content.size * 2)
        content.forEachIndexed { index, (header, item) ->
            list += RationaleModel.Header(header)
            if (index !in collapsedIndex) {
                list += RationaleModel.Item(item)
            }
        }
        submitList(list)
    }

    class HeaderViewHolder(val binding: RationaleHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class ItemViewHolder(val binding: RationaleItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<RationaleModel>() {
            override fun areItemsTheSame(
                oldItem: RationaleModel, newItem: RationaleModel
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: RationaleModel, newItem: RationaleModel
            ): Boolean =
                if (oldItem is RationaleModel.Header && newItem is RationaleModel.Header) false
                else oldItem == newItem
        }
    }
}

sealed class RationaleModel(open val text: String) {
    data class Header(override val text: String) : RationaleModel(text)
    data class Item(override val text: String) : RationaleModel(text)
}
