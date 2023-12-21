package me.gm.cleaner.widget.recyclerview;

import androidx.annotation.Nullable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class DiffArrayList<E> extends ObservableArrayList<E> {
    private final ArrayList<Consumer<RecyclerView.Adapter<?>>> mPendingUpdates = new ArrayList<>();

    public DiffArrayList() {
        super();
        init();
    }

    public DiffArrayList(final Collection<? extends E> c) {
        super();
        init();
        addAll(c);
    }

    public void init() {
        addOnListChangedCallback(new OnListChangedCallback<>() {
            @Override
            public void onChanged(final ObservableList sender) {
                mPendingUpdates.add(RecyclerView.Adapter::notifyDataSetChanged);
            }

            @Override
            public void onItemRangeChanged(final ObservableList sender, final int positionStart,
                                           final int itemCount) {
                mPendingUpdates.add(adapter -> adapter.notifyItemRangeChanged(positionStart, itemCount));
            }

            @Override
            public void onItemRangeInserted(final ObservableList sender, final int positionStart,
                                            final int itemCount) {
                mPendingUpdates.add(adapter -> adapter.notifyItemRangeInserted(positionStart, itemCount));
            }

            @Override
            public void onItemRangeMoved(final ObservableList sender, final int fromPosition,
                                         final int toPosition, final int itemCount) {
                mPendingUpdates.add(adapter -> adapter.notifyItemMoved(fromPosition, toPosition));
            }

            @Override
            public void onItemRangeRemoved(final ObservableList sender, final int positionStart,
                                           final int itemCount) {
                mPendingUpdates.add(adapter -> adapter.notifyItemRangeRemoved(positionStart, itemCount));
            }
        });
    }

    public boolean hasPendingUpdates() {
        return !mPendingUpdates.isEmpty();
    }

    public void consumePendingUpdates(final @Nullable RecyclerView.Adapter<?> adapter) {
        if (adapter != null) {
            for (final Consumer<RecyclerView.Adapter<?>> pendingChange : mPendingUpdates) {
                pendingChange.accept(adapter);
            }
        }
        mPendingUpdates.clear();
    }
}
