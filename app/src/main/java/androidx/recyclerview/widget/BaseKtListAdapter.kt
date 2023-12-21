package androidx.recyclerview.widget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

abstract class BaseKtListAdapter<T, VH : RecyclerView.ViewHolder> : BaseListAdapter<T, VH> {
    protected constructor (diffCallback: DiffUtil.ItemCallback<T>) : super(
        AsyncDifferConfig.Builder(diffCallback)
            .setMainThreadExecutor(Dispatchers.Main.immediate.asExecutor())
            .setBackgroundThreadExecutor(Dispatchers.Default.asExecutor())
            .build()
    )
}
