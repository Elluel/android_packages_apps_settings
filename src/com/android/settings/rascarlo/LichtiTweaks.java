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

public class LichtiTweaks extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "LichtiTweaks";

    private static final String KEY_PEEK = "notification_peek";
    private static final String KEY_PEEK_PICKUP_TIMEOUT = "peek_pickup_timeout";

    private CheckBoxPreference mNotificationPeek;
    private ListPreference mPeekPickupTimeout;

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
        int peekTimeout = Settings.System.getInt(getContentResolver(),
                Settings.System.PEEK_PICKUP_TIMEOUT, 10000);
        mPeekPickupTimeout.setValue(String.valueOf(peekTimeout));
        mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntry());
        mPeekPickupTimeout.setOnPreferenceChangeListener(this);
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
            int peekTimeout = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PEEK_PICKUP_TIMEOUT, peekTimeout);
            mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntries()[index]);
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
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
