package androidx.recyclerview.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public abstract class BaseListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    final SimpleListDiffer<T> mDiffer;
    private final SimpleListDiffer.ListListener<T> mListener = this::onCurrentListChanged;

    @SuppressWarnings("unused")
    protected BaseListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        mDiffer = new SimpleListDiffer<>(new AdapterListUpdateCallback(this),
                new AsyncDifferConfig.Builder<>(diffCallback).build());
        mDiffer.addListListener(mListener);
    }

    @SuppressWarnings("unused")
    protected BaseListAdapter(@NonNull AsyncDifferConfig<T> config) {
        mDiffer = new SimpleListDiffer<>(new AdapterListUpdateCallback(this), config);
        mDiffer.addListListener(mListener);
    }

    public void setCurrentList(@Nullable List<T> list) {
        mDiffer.setCurrentList(list);
    }

    public void setCurrentList(@Nullable List<T> list, @Nullable final Runnable commitCallback) {
        mDiffer.setCurrentList(list, commitCallback);
    }

    /**
     * Submits a new list to be diffed, and displayed.
     * <p>
     * If a list is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param list The new list to be displayed.
     */
    public void submitList(@Nullable List<T> list) {
        mDiffer.submitList(list);
    }

    /**
     * Set the new list to be displayed.
     * <p>
     * If a List is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     * <p>
     * The commit callback can be used to know when the List is committed, but note that it
     * may not be executed. If List B is submitted immediately after List A, and is
     * committed directly, the callback associated with List A will not be run.
     *
     * @param list           The new list to be displayed.
     * @param commitCallback Optional runnable that is executed when the List is committed, if
     *                       it is committed.
     */
    public void submitList(@Nullable List<T> list, @Nullable final Runnable commitCallback) {
        mDiffer.submitList(list, commitCallback);
    }

    protected T getItem(int position) {
        return mDiffer.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    /**
     * Get the current List - any diffing to present this list has already been computed and
     * dispatched via the ListUpdateCallback.
     * <p>
     * If a <code>null</code> List, or no List has been submitted, an empty list will be returned.
     * <p>
     * The returned list may not be mutated - mutations to content must be done through
     * {@link #submitList(List)}.
     *
     * @return The list currently being displayed.
     * @see #onCurrentListChanged(List, List)
     */
    @NonNull
    public List<T> getCurrentList() {
        return mDiffer.getCurrentList();
    }

    /**
     * Called when the current List is updated.
     * <p>
     * If a <code>null</code> List is passed to {@link #submitList(List)}, or no List has been
     * submitted, the current List is represented as an empty List.
     *
     * @param previousList List that was displayed previously.
     * @param currentList  new List being displayed, will be empty if {@code null} was passed to
     *                     {@link #submitList(List)}.
     * @see #getCurrentList()
     */
    public void onCurrentListChanged(@NonNull List<T> previousList, @NonNull List<T> currentList) {
    }
}
