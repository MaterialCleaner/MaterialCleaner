package me.gm.cleaner.settings.theme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;

import com.takisoft.preferencex.colorpicker.R;

public class ThemeColorPreference extends DialogPreference {
    private final ThemeUtil.CustomThemeColors[] colors;
    private ThemeUtil.CustomThemeColors color;

    private ImageView colorWidget;

    public ThemeColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        colors = ThemeUtil.CustomThemeColors.values();

        setWidgetLayoutResource(R.layout.preference_widget_color_swatch);
    }

    public ThemeColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressLint("RestrictedApi")
    public ThemeColorPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle));
    }

    public ThemeColorPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        colorWidget = (ImageView) holder.findViewById(R.id.color_picker_widget);
        setColorOnWidget(color);
    }

    private void setColorOnWidget(ThemeUtil.CustomThemeColors color) {
        if (colorWidget == null) {
            return;
        }
        Drawable drawable = getContext().getDrawable(R.drawable.colorpickerpreference_pref_swatch);
        drawable.setTint(getContext().getColor(color.getResourceId()));
        colorWidget.setImageDrawable(drawable);
        colorWidget.setAlpha(isEnabled() ? 1F : 0.5F);
    }

    public ThemeUtil.CustomThemeColors[] getColors() {
        return colors;
    }

    private void setColorInternal(ThemeUtil.CustomThemeColors color, boolean force) {
        ThemeUtil.CustomThemeColors oldColor = ThemeUtil.CustomThemeColors.valueOf(
                getPersistedString("COLOR_PRIMARY"));

        boolean changed = !oldColor.equals(color);

        if (changed || force) {
            this.color = color;

            persistString(color.toString());

            setColorOnWidget(color);

            notifyChanged();
        }
    }

    public ThemeUtil.CustomThemeColors getColor() {
        return color;
    }

    public void setColor(ThemeUtil.CustomThemeColors color) {
        setColorInternal(color, false);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(Object defaultValueObj) {
        setColorInternal(ThemeUtil.CustomThemeColors.valueOf(getPersistedString(ThemeUtil
                .CustomThemeColors.COLOR_PRIMARY.toString())), true);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.color = color.toString();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setColor(ThemeUtil.CustomThemeColors.valueOf(myState.color));
    }

    private static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        private String color;

        public SavedState(Parcel source) {
            super(source);
            color = source.readString();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(color);
        }
    }
}
