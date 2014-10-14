/*
* Copyright (C) 2010 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.settings.rascarlo;

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.rascarlo.SeekBarPreference;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class LichtiTweaks extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "LichtiTweaks";

    // Lockscreen Tweaks
    private static final String KEY_ENABLE_POWER_MENU = "lockscreen_enable_power_menu";
    private static final String KEY_SEE_THROUGH = "lockscreen_see_through";
    private static final String KEY_BLUR_RADIUS = "lockscreen_blur_radius";
    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";

    // Miui style carrier in statusbar
    private static final String STATUS_BAR_CARRIER = "status_bar_carrier";
    private static final String STATUS_BAR_CARRIER_COLOR = "status_bar_carrier_color";
    static final int DEFAULT_STATUS_CARRIER_COLOR = 0xffffffff;

    // Tinted statusbar
    private static final String TINTED_STATUSBAR = "tinted_statusbar";
    private static final String TINTED_STATUSBAR_OPTION = "tinted_statusbar_option";
    private static final String TINTED_STATUSBAR_FILTER = "status_bar_tinted_filter";
    private static final String TINTED_STATUSBAR_TRANSPARENT = "tinted_statusbar_transparent";
    private static final String TINTED_NAVBAR_TRANSPARENT = "tinted_navbar_transparent";
    private static final String CATEGORY_TINTED = "category_tinted_statusbar";

    // Lockscreen Tweaks
    private CheckBoxPreference mEnablePowerMenu;
    private CheckBoxPreference mSeeThrough;
    private SeekBarPreference mBlurRadius;
    private CheckBoxPreference mLockRingBattery;
    // Miui style carrier in statusbar
    private CheckBoxPreference mStatusBarCarrier;
    private ColorPickerPreference mCarrierColorPicker;
    // Tinted statusbar
    private ListPreference mTintedStatusbar;
    private ListPreference mTintedStatusbarOption;
    private CheckBoxPreference mTintedStatusbarFilter;
    private SeekBarPreference mTintedStatusbarTransparency;
    private SeekBarPreference mTintedNavbarTransparency;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lichti_tweaks);

        PackageManager pm = getPackageManager();

        int intColor;
        String hexColor;

        // Enable / disable power menu on lockscreen
        mEnablePowerMenu = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(KEY_ENABLE_POWER_MENU);
        mEnablePowerMenu.setChecked(Settings.System.getInt(getActivity()
                .getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_ENABLE_POWER_MENU, 1) == 1);

        // Lockscreen see through
        mSeeThrough = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(KEY_SEE_THROUGH);
        if (mSeeThrough != null) {
            mSeeThrough.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, 0) == 1);
            mSeeThrough.setOnPreferenceChangeListener(this);
        }

        // Lockscreen Blur
        mBlurRadius = (SeekBarPreference) findPreference(KEY_BLUR_RADIUS);
        mBlurRadius.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
        mBlurRadius.setOnPreferenceChangeListener(this);

        // Battery around unlockring
        mLockRingBattery = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(BATTERY_AROUND_LOCKSCREEN_RING);
        if (mLockRingBattery != null) {
            mLockRingBattery.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, 0) == 1);
        }

        // MIUI-like carrier Label
        mStatusBarCarrier = (CheckBoxPreference) findPreference(STATUS_BAR_CARRIER);
        mStatusBarCarrier.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CARRIER, 0) == 1));
        // MIUI-like carrier Label color
        mCarrierColorPicker = (ColorPickerPreference) findPreference(STATUS_BAR_CARRIER_COLOR);
        mCarrierColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CARRIER_COLOR, DEFAULT_STATUS_CARRIER_COLOR);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mCarrierColorPicker.setSummary(hexColor);
        mCarrierColorPicker.setNewPreviewColor(intColor);

        // Tinted statusbar
        final PreferenceCategory tintedCategory = (PreferenceCategory) getPreferenceScreen()
                .findPreference(CATEGORY_TINTED);

        mTintedStatusbar = (ListPreference) findPreference(TINTED_STATUSBAR);
        int tintedStatusbar = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_COLOR, 0);
        mTintedStatusbar.setValue(String.valueOf(tintedStatusbar));
        mTintedStatusbar.setSummary(mTintedStatusbar.getEntry());
        mTintedStatusbar.setOnPreferenceChangeListener(this);

        mTintedStatusbarFilter = (CheckBoxPreference) findPreference(TINTED_STATUSBAR_FILTER);
        mTintedStatusbarFilter.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_FILTER, 0) == 1));

        mTintedStatusbarTransparency = (SeekBarPreference) findPreference(TINTED_STATUSBAR_TRANSPARENT);
        mTintedStatusbarTransparency.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_STATBAR_TRANSPARENT, 100));
        mTintedStatusbarTransparency.setEnabled(tintedStatusbar != 0);
        mTintedStatusbarTransparency.setOnPreferenceChangeListener(this);

        mTintedStatusbarOption = (ListPreference) findPreference(TINTED_STATUSBAR_OPTION);
        mTintedNavbarTransparency = (SeekBarPreference) findPreference(TINTED_NAVBAR_TRANSPARENT);

        int tintedStatusbarOption = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_OPTION, 0);
        mTintedStatusbarOption.setValue(String.valueOf(tintedStatusbarOption));
        mTintedStatusbarOption.setSummary(mTintedStatusbarOption.getEntry());
        mTintedStatusbarOption.setEnabled(tintedStatusbar != 0);
        mTintedStatusbarOption.setOnPreferenceChangeListener(this);

        mTintedNavbarTransparency.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_NAVBAR_TRANSPARENT, 100));
        mTintedNavbarTransparency.setEnabled(tintedStatusbar != 0);
        mTintedNavbarTransparency.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mSeeThrough) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, value ? 1 : 0);
            return true;
        } else if (preference == mBlurRadius) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer) objValue);
            return true;
        } else if (preference == mCarrierColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_CARRIER_COLOR, intHex);
            return true;
        } else if (preference == mTintedStatusbar) {
            int val = Integer.parseInt((String) objValue);
            int index = mTintedStatusbar.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_TINTED_COLOR, val);
            mTintedStatusbar.setSummary(mTintedStatusbar.getEntries()[index]);
            if (mTintedStatusbarOption != null) {
                mTintedStatusbarOption.setEnabled(val != 0);
            }
            mTintedStatusbarFilter.setEnabled(val != 0);
            mTintedStatusbarTransparency.setEnabled(val != 0);
            if (mTintedNavbarTransparency != null) {
                mTintedNavbarTransparency.setEnabled(val != 0);
            }
        } else if (preference == mTintedStatusbarOption) {
            int val = Integer.parseInt((String) objValue);
            int index = mTintedStatusbarOption.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_TINTED_OPTION, val);
            mTintedStatusbarOption.setSummary(mTintedStatusbarOption.getEntries()[index]);
        } else if (preference == mTintedStatusbarTransparency) {
            int val = ((Integer)objValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_TINTED_STATBAR_TRANSPARENT, val);
            return true;
        } else if (preference == mTintedNavbarTransparency) {
            int val = ((Integer)objValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_TINTED_NAVBAR_TRANSPARENT, val);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mEnablePowerMenu) {
            value = mEnablePowerMenu.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_ENABLE_POWER_MENU, value ? 1 : 0);
            return true;
        } else if (preference == mLockRingBattery) {
            value = mLockRingBattery.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarCarrier) {
            value = mStatusBarCarrier.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_CARRIER, value ? 1 : 0);
            return true;
        } else if (preference == mTintedStatusbarFilter) {
            value = mTintedStatusbarFilter.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_TINTED_FILTER, value ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
