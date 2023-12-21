package androidx.recyclerview.widget;

import android.content.Context;

/**
 * A {@link GridLayoutManager} that always layout all items.
 */
public class InfiniteGridLayoutManager extends GridLayoutManager {

    public InfiniteGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    @Override
    boolean resolveIsInfinite() {
        return true;
    }
}
