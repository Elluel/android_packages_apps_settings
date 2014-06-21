package com.android.settings.rascarlo;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NavigationBarSettings extends SettingsPreferenceFragment implements
OnPreferenceChangeListener {

    private static final String KEY_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String NAVBAR_BUTTON_TINT = "navbar_button_tint";
    private static final String PREF_RECENT_LONG_PRESS = "recent_long_press";

    private ListPreference mNavigationBarHeight;
    private ColorPickerPreference mNavbarButtonTint;
    private ListPreference mRecentLongPress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.navigation_bar_settings);

        mNavigationBarHeight = (ListPreference) findPreference(KEY_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setOnPreferenceChangeListener(this);
        int statusNavigationBarHeight = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(),
                Settings.System.NAVIGATION_BAR_HEIGHT, 48);
        mNavigationBarHeight.setValue(String.valueOf(statusNavigationBarHeight));
        mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntry());

        mNavbarButtonTint = (ColorPickerPreference) findPreference(NAVBAR_BUTTON_TINT);
        mNavbarButtonTint.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                   Settings.System.NAVIGATION_BAR_TINT, 0xffffffff);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
        mNavbarButtonTint.setSummary(hexColor);
        mNavbarButtonTint.setNewPreviewColor(intColor);

        mRecentLongPress = (ListPreference) findPreference(PREF_RECENT_LONG_PRESS);
        int longPress = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NAVBAR_RECENT_LONG_PRESS, 0, UserHandle.USER_CURRENT);
        mRecentLongPress.setValue(String.valueOf(longPress));
        mRecentLongPress.setOnPreferenceChangeListener(this);
        updateLongPressMode(longPress);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNavigationBarHeight) {
            int statusNavigationBarHeight = Integer.valueOf((String) objValue);
            int index = mNavigationBarHeight.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, statusNavigationBarHeight);
            mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntries()[index]);
            return true;

        } else if (preference == mNavbarButtonTint) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
            Settings.System.NAVIGATION_BAR_TINT, intHex);
            return true;

        } else if (preference == mRecentLongPress) {
            int longPress = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NAVBAR_RECENT_LONG_PRESS,
                    longPress, UserHandle.USER_CURRENT);
            updateLongPressMode(longPress);
            return true;
        }
        return false;
    }

    private void updateLongPressMode(int value) {
        ContentResolver cr = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, value);

        if (value == 0) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 0);
            summary = R.string.recent_long_press_none;
        } else if (value == 1) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 1);
            summary = R.string.recent_long_press_last_app;
        } else if (value == 2) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 2);
            summary = R.string.recent_long_press_screenshot;
        } else if (value == 3) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 3);
            summary = R.string.recent_long_press_kill_app;
        } else if (value == 4) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 4);
            summary = R.string.recent_long_press_notif_panel;
        } else if (value == 5) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 5);
            summary = R.string.recent_long_press_qs_panel;
        } else if (value == 6) {
            Settings.System.putInt(cr, Settings.System.NAVBAR_RECENT_LONG_PRESS, 6);
            summary = R.string.recent_long_press_power_menu;
        }

        if (mRecentLongPress != null && summary != -1) {
            mRecentLongPress.setSummary(res.getString(summary));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
