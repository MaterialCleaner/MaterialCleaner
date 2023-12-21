package me.gm.cleaner.settings.theme;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.takisoft.colorpicker.ColorPickerDialog;
import com.takisoft.colorpicker.OnColorSelectedListener;

import me.gm.cleaner.util.RecyclerViewKt;

public class ThemeColorPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat
        implements OnColorSelectedListener {

    ThemeUtil.CustomThemeColors[] themeColors;
    private int pickedColor;
    private int[] colors;

    public static ThemeColorPreferenceDialogFragmentCompat newInstance(String key) {
        final var fragment = new ThemeColorPreferenceDialogFragmentCompat();
        final var b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ThemeColorPreference pref = getColorPickerPreference();
        int selectedColor = requireActivity().getColor(pref.getColor().getResourceId());
        themeColors = pref.getColors();
        colors = new int[themeColors.length];
        for (int i = 0; i < themeColors.length; i++) {
            colors[i] = requireActivity().getColor(themeColors[i].getResourceId());
        }

        ColorPickerDialog.Params params = new ColorPickerDialog.Params.Builder(requireActivity())
                .setSelectedColor(selectedColor)
                .setColors(colors)
                .setSize(ColorPickerDialog.SIZE_SMALL)
                .setSortColors(false)
                .setColumns(0)
                .build();

        ColorPickerDialog dialog = new ColorPickerDialog(requireActivity(), this, params);
        dialog.setTitle(pref.getDialogTitle());
        try {
            for (final var field : dialog.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(dialog) instanceof final RecyclerView palette) {
                    assert palette != null;
                    palette.setLayoutManager(new GridLayoutManager(requireContext(), 5));
                    RecyclerViewKt.fixEdgeEffect(palette, true);
                    RecyclerViewKt.overScrollIfContentScrollsPersistent(palette, true);
                    break;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return dialog;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        ThemeColorPreference preference = getColorPickerPreference();

        if (positiveResult && preference.callChangeListener(pickedColor)) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] == pickedColor) {
                    preference.setColor(themeColors[i]);
                }
            }
        }
    }

    @Override
    public void onColorSelected(int color) {
        this.pickedColor = color;

        super.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
    }

    ThemeColorPreference getColorPickerPreference() {
        return (ThemeColorPreference) getPreference();
    }
}
