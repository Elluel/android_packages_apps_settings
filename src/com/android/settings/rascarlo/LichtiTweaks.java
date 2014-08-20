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
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.rascarlo.SeekBarPreference;

public class LichtiTweaks extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "LichtiTweaks";

    // Lockscreen Tweaks
    private static final String KEY_ENABLE_POWER_MENU = "lockscreen_enable_power_menu";
    private static final String KEY_SEE_THROUGH = "lockscreen_see_through";
    private static final String KEY_BLUR_RADIUS = "lockscreen_blur_radius";
    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";

    // Default timeout for heads up snooze. 5 minutes.
    protected static final int DEFAULT_TIME_HEADS_UP_SNOOZE = 300000;

    private static final String PREF_HEADS_UP_EXPANDED =
            "heads_up_expanded";
    private static final String PREF_HEADS_UP_GRAVITY =
            "heads_up_gravity";
    private static final String PREF_HEADS_UP_SNOOZE_TIME =
            "heads_up_snooze_time";
    private static final String PREF_HEADS_UP_TIME_OUT =
            "heads_up_time_out";
    private static final String PREF_HEADS_UP_SHOW_UPDATE =
            "heads_up_show_update";

    // Kernel Tweaks
    private static final String KEY_TOUCH_CONTROL_SETTINGS = "touch_control_settings";
    private static final String KEY_TOUCH_CONTROL_PACKAGE_NAME = "com.mahdi.touchcontrol";

    // Lockscreen Tweaks
    private CheckBoxPreference mEnablePowerMenu;
    private CheckBoxPreference mSeeThrough;
    private SeekBarPreference mBlurRadius;
    private CheckBoxPreference mLockRingBattery;
    // Heads up
    private ListPreference mHeadsUpSnoozeTime;
    private ListPreference mHeadsUpTimeOut;
    private CheckBoxPreference mHeadsUpExpanded;
    private CheckBoxPreference mHeadsUpShowUpdates;
    private CheckBoxPreference mHeadsUpGravity;
    // Kernel Tweaks
    private PreferenceScreen mTouchControl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lichti_tweaks);

        PackageManager pm = getPackageManager();

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

        // Heads up
        mHeadsUpExpanded = (CheckBoxPreference) findPreference(PREF_HEADS_UP_EXPANDED);
        mHeadsUpExpanded.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_EXPANDED, 0) == 1);
        mHeadsUpExpanded.setOnPreferenceChangeListener(this);

        mHeadsUpShowUpdates = (CheckBoxPreference) findPreference(PREF_HEADS_UP_SHOW_UPDATE);
        mHeadsUpShowUpdates.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_SHOW_UPDATE, 0) == 1);
        mHeadsUpShowUpdates.setOnPreferenceChangeListener(this);

        mHeadsUpGravity = (CheckBoxPreference) findPreference(PREF_HEADS_UP_GRAVITY);
        mHeadsUpGravity.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_GRAVITY_BOTTOM, 0) == 1);
        mHeadsUpGravity.setOnPreferenceChangeListener(this);

        mHeadsUpSnoozeTime = (ListPreference) findPreference(PREF_HEADS_UP_SNOOZE_TIME);
        mHeadsUpSnoozeTime.setOnPreferenceChangeListener(this);
        int headsUpSnoozeTime = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_SNOOZE_TIME, DEFAULT_TIME_HEADS_UP_SNOOZE);
        mHeadsUpSnoozeTime.setValue(String.valueOf(headsUpSnoozeTime));
        updateHeadsUpSnoozeTimeSummary(headsUpSnoozeTime);

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            return;
        }

        int defaultTimeOut = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_notification_decay", null, null));
        mHeadsUpTimeOut = (ListPreference) findPreference(PREF_HEADS_UP_TIME_OUT);
        mHeadsUpTimeOut.setOnPreferenceChangeListener(this);
        int headsUpTimeOut = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_NOTIFCATION_DECAY, defaultTimeOut);
        mHeadsUpTimeOut.setValue(String.valueOf(headsUpTimeOut));
        updateHeadsUpTimeOutSummary(headsUpTimeOut);

        // Kernel Tweaks
        mTouchControl = (PreferenceScreen) findPreference(KEY_TOUCH_CONTROL_SETTINGS);
        if (!Utils.isPackageInstalled(getActivity(), KEY_TOUCH_CONTROL_PACKAGE_NAME)) {
                getPreferenceScreen().removePreference(mTouchControl);
        }
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
        } else if (preference == mHeadsUpSnoozeTime) {
            int headsUpSnoozeTime = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_SNOOZE_TIME,
                    headsUpSnoozeTime);
            updateHeadsUpSnoozeTimeSummary(headsUpSnoozeTime);
            return true;
        } else if (preference == mHeadsUpTimeOut) {
            int headsUpTimeOut = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_NOTIFCATION_DECAY,
                    headsUpTimeOut);
            updateHeadsUpTimeOutSummary(headsUpTimeOut);
            return true;
        } else if (preference == mHeadsUpExpanded) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_EXPANDED, value ? 1 : 0);
            return true;
        } else if (preference == mHeadsUpShowUpdates) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_SHOW_UPDATE, value ? 1 : 0);
            return true;
        } else if (preference == mHeadsUpGravity) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_GRAVITY_BOTTOM, value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateHeadsUpSnoozeTimeSummary(int value) {
        String summary = value != 0
                ? getResources().getString(R.string.heads_up_snooze_summary, value / 60 / 1000)
                : getResources().getString(R.string.heads_up_snooze_disabled_summary);
        mHeadsUpSnoozeTime.setSummary(summary);
    }

    private void updateHeadsUpTimeOutSummary(int value) {
        String summary = getResources().getString(R.string.heads_up_time_out_summary,
                value / 1000);
        if (value == 0) {
            mHeadsUpTimeOut.setSummary(
                    getResources().getString(R.string.heads_up_time_out_never_summary));
        } else {
            mHeadsUpTimeOut.setSummary(summary);
        }
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
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
