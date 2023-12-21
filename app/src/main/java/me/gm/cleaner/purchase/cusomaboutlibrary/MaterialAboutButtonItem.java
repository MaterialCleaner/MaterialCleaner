package me.gm.cleaner.purchase.cusomaboutlibrary;

import static android.view.View.GONE;

import android.content.Context;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.StringRes;

import com.danielstone.materialaboutlibrary.R;
import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.google.android.material.button.MaterialButton;

public class MaterialAboutButtonItem extends MaterialAboutItem {
    public static final int BUTTON_ITEM = 2;

    private CharSequence text = null;
    private int textRes = 0;
    private boolean isEnabled = true;
    private MaterialAboutItemOnClickAction onClickAction = null;
    private MaterialAboutItemOnClickAction onLongClickAction = null;

    private MaterialAboutButtonItem(final Builder builder) {
        super();
        this.text = builder.text;
        this.textRes = builder.textRes;
        this.isEnabled = builder.isEnabled;

        this.onClickAction = builder.onClickAction;
        this.onLongClickAction = builder.onLongClickAction;
    }

    public MaterialAboutButtonItem(final CharSequence text, final MaterialAboutItemOnClickAction onClickAction) {
        this.text = text;
        this.onClickAction = onClickAction;
    }

    public MaterialAboutButtonItem(final CharSequence text) {
        this.text = text;
    }

    public MaterialAboutButtonItem(final int textRes, final MaterialAboutItemOnClickAction onClickAction) {
        this.textRes = textRes;
        this.onClickAction = onClickAction;
    }

    public MaterialAboutButtonItem(final int textRes) {
        this.textRes = textRes;
    }

    public static MaterialAboutItemViewHolder getViewHolder(final View view) {
        return new MaterialAboutButtonItemViewHolder(view);
    }

    public static void setupItem(final MaterialAboutButtonItemViewHolder holder, final MaterialAboutButtonItem item, final Context context) {
        final CharSequence text = item.getText();
        final int textRes = item.getTextRes();

        holder.text.setVisibility(View.VISIBLE);
        if (text != null) {
            holder.text.setText(text);
        } else if (textRes != 0) {
            holder.text.setText(textRes);
        } else {
            holder.text.setVisibility(GONE);
        }
        holder.text.setEnabled(item.getIsEnabled());

        int pL = 0, pT = 0, pR = 0, pB = 0;
        if (Build.VERSION.SDK_INT < 21) {
            pL = holder.view.getPaddingLeft();
            pT = holder.view.getPaddingTop();
            pR = holder.view.getPaddingRight();
            pB = holder.view.getPaddingBottom();
        }

        if (item.getOnClickAction() != null || item.getOnLongClickAction() != null) {
            final TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            holder.view.setBackgroundResource(outValue.resourceId);
        } else {
            holder.view.setBackgroundResource(0);
        }
        holder.setOnClickAction(item.getOnClickAction());
        holder.setOnLongClickAction(item.getOnLongClickAction());

        if (Build.VERSION.SDK_INT < 21) {
            holder.view.setPadding(pL, pT, pR, pB);
        }
    }

    @Override
    public int getType() {
        return BUTTON_ITEM;
    }

    @Override
    public String getDetailString() {
        return "MaterialAboutButtonItem{" +
                "text=" + text +
                ", textRes=" + textRes +
                ", onClickAction=" + onClickAction +
                ", onLongClickAction=" + onLongClickAction +
                '}';
    }

    public MaterialAboutButtonItem(final MaterialAboutButtonItem item) {
        this.id = item.getId();
        this.text = item.getText();
        this.textRes = item.getTextRes();
        this.isEnabled = item.getIsEnabled();
        this.onClickAction = item.onClickAction;
        this.onLongClickAction = item.onLongClickAction;
    }

    @Override
    public MaterialAboutItem clone() {
        return new MaterialAboutButtonItem(this);
    }

    public CharSequence getText() {
        return text;
    }

    public MaterialAboutButtonItem setText(final CharSequence text) {
        this.textRes = 0;
        this.text = text;
        return this;
    }

    public int getTextRes() {
        return textRes;
    }

    public MaterialAboutButtonItem setTextRes(final int textRes) {
        this.text = null;
        this.textRes = textRes;
        return this;
    }

    public boolean getIsEnabled() {
        return isEnabled;
    }

    public MaterialAboutItemOnClickAction getOnClickAction() {
        return onClickAction;
    }

    public MaterialAboutButtonItem setOnClickAction(final MaterialAboutItemOnClickAction onClickAction) {
        this.onClickAction = onClickAction;
        return this;
    }

    public MaterialAboutItemOnClickAction getOnLongClickAction() {
        return onLongClickAction;
    }

    public MaterialAboutButtonItem setOnLongClickAction(final MaterialAboutItemOnClickAction onLongClickAction) {
        this.onLongClickAction = onLongClickAction;
        return this;
    }

    public static class MaterialAboutButtonItemViewHolder extends MaterialAboutItemViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public final View view;
        public final MaterialButton text;
        private MaterialAboutItemOnClickAction onClickAction;
        private MaterialAboutItemOnClickAction onLongClickAction;

        MaterialAboutButtonItemViewHolder(final View view) {
            super(view);
            this.view = view;
            text = view.findViewById(R.id.mal_item_text);
        }

        public void setOnClickAction(final MaterialAboutItemOnClickAction onClickAction) {
            this.onClickAction = onClickAction;
            text.setOnClickListener(onClickAction != null ? this : null);
        }

        public void setOnLongClickAction(final MaterialAboutItemOnClickAction onLongClickAction) {
            this.onLongClickAction = onLongClickAction;
            text.setOnLongClickListener(onLongClickAction != null ? this : null);
        }

        @Override
        public void onClick(final View v) {
            if (onClickAction != null) {
                onClickAction.onClick();
            }
        }

        @Override
        public boolean onLongClick(final View v) {
            if (onLongClickAction != null) {
                onLongClickAction.onClick();
                return true;
            }
            return false;
        }
    }

    public static class Builder {

        MaterialAboutItemOnClickAction onClickAction = null;
        MaterialAboutItemOnClickAction onLongClickAction = null;
        private CharSequence text = null;
        @StringRes
        private int textRes = 0;
        private boolean isEnabled = true;

        public Builder text(final CharSequence text) {
            this.text = text;
            this.textRes = 0;
            return this;
        }

        public Builder text(@StringRes final int text) {
            this.textRes = text;
            this.text = null;
            return this;
        }

        public Builder enabled(final boolean isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        public Builder setOnClickAction(final MaterialAboutItemOnClickAction onClickAction) {
            this.onClickAction = onClickAction;
            return this;
        }

        public Builder setOnLongClickAction(final MaterialAboutItemOnClickAction onLongClickAction) {
            this.onLongClickAction = onLongClickAction;
            return this;
        }

        public MaterialAboutButtonItem build() {
            return new MaterialAboutButtonItem(this);
        }
    }
}
