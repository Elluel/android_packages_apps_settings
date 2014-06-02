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

    // Peek
    private static final String KEY_PEEK = "notification_peek";
    private static final String KEY_PEEK_PICKUP_TIMEOUT = "peek_pickup_timeout";
    private static final String KEY_PEEK_WAKE_TIMEOUT = "peek_wake_timeout";
    // Lockscreen Tweaks
    private static final String KEY_ENABLE_POWER_MENU = "lockscreen_enable_power_menu";
    private static final String KEY_SEE_THROUGH = "lockscreen_see_through";
    private static final String KEY_BLUR_RADIUS = "lockscreen_blur_radius";

    // Peek
    private CheckBoxPreference mNotificationPeek;
    private ListPreference mPeekPickupTimeout;
    private ListPreference mPeekWakeTimeout;
    // Lockscreen Tweaks
    private CheckBoxPreference mEnablePowerMenu;
    private CheckBoxPreference mSeeThrough;
    private SeekBarPreference mBlurRadius;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lichti_tweaks);

        // Peek
        mNotificationPeek = (CheckBoxPreference) getPreferenceScreen() 
                .findPreference(KEY_PEEK);
        mNotificationPeek.setChecked((Settings.System.getInt(getActivity()
                .getApplicationContext().getContentResolver(),
                Settings.System.PEEK_STATE, 0) == 1));

        // Peek pickup timeout
        mPeekPickupTimeout = (ListPreference) getPreferenceScreen()
                .findPreference(KEY_PEEK_PICKUP_TIMEOUT);
        int peekPickupTimeout = Settings.System.getInt(getContentResolver(),
                Settings.System.PEEK_PICKUP_TIMEOUT, 10000);
        mPeekPickupTimeout.setValue(String.valueOf(peekPickupTimeout));
        mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntry());
        mPeekPickupTimeout.setOnPreferenceChangeListener(this);

        // Peek wake timeout
        mPeekWakeTimeout = (ListPreference) getPreferenceScreen()
                .findPreference(KEY_PEEK_WAKE_TIMEOUT);
        int peekWakeTimeout = Settings.System.getInt(getContentResolver(),
                Settings.System.PEEK_WAKE_TIMEOUT, 5000);
        mPeekWakeTimeout.setValue(String.valueOf(peekWakeTimeout));
        mPeekWakeTimeout.setSummary(mPeekWakeTimeout.getEntry());
        mPeekWakeTimeout.setOnPreferenceChangeListener(this);

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
        if (preference == mPeekPickupTimeout) {
            int index = mPeekPickupTimeout.findIndexOfValue((String) objValue);
            int peekPickupTimeout = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PEEK_PICKUP_TIMEOUT, peekPickupTimeout);
            mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntries()[index]);
            return true;
        } else if (preference == mPeekWakeTimeout) {
            int index = mPeekWakeTimeout.findIndexOfValue((String) objValue);
            int peekWakeTimeout = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PEEK_WAKE_TIMEOUT, peekWakeTimeout);
            mPeekWakeTimeout.setSummary(mPeekWakeTimeout.getEntries()[index]);
            return true;
        } else if (preference == mSeeThrough) {
            boolean objValue = (Boolean) value;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, objValue ? 1 : 0);
            return true;
        } else if (preference == mBlurRadius) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer) objValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mNotificationPeek) {
            value = mNotificationPeek.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), 
                    Settings.System.PEEK_STATE, value ? 1 : 0);
            return true;
        } else if (preference == mEnablePowerMenu) {
            value = mEnablePowerMenu.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_ENABLE_POWER_MENU, value ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
