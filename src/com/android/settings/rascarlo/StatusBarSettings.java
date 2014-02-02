
package com.android.settings.rascarlo;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.rascarlo.SeekBarPreference;

public class StatusBarSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    // General
    private static String STATUS_BAR_GENERAL_CATEGORY = "status_bar_general_category";
    // Brightness control
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    // Double-tap to sleep
    private static final String DOUBLE_TAP_SLEEP_GESTURE = "double_tap_sleep_gesture";
    // Network traffic indicator
    private static final String NETWORK_STATS = "network_stats";
    private static final String NETWORK_STATS_UPDATE_FREQUENCY = "network_stats_update_frequency";
    // Status bar battery style
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    // Clock
    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock_style";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    // Quick Settings
    private static final String QUICK_SETTINGS_CATEGORY = "status_bar_quick_settings_category";
    private static final String QUICK_PULLDOWN = "quick_pulldown";

    // General
    private PreferenceCategory mStatusBarGeneralCategory;
    // Status bar battery style
    private ListPreference mStatusBarBattery;
    // Brightness control
    private CheckBoxPreference mStatusBarBrightnessControl;
    // Double-tap to sleep
    private CheckBoxPreference mStatusBarDoubleTapSleepGesture;
    // Network traffic indicator
    private CheckBoxPreference mNetworkStats;
    private SeekBarPreference mNetworkStatsUpdateFrequency;
    // Clock
    private ListPreference mStatusBarAmPm;
    private ListPreference mStatusBarClockStyle;
    // Quick Settings
    private ListPreference mQuickPulldown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_settings);

            // General category
            mStatusBarGeneralCategory = (PreferenceCategory) findPreference(STATUS_BAR_GENERAL_CATEGORY);
            mStatusBarBrightnessControl = (CheckBoxPreference) getPreferenceScreen().findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
            // only show on phones
            if (!Utils.isPhone(getActivity())) {
                mStatusBarGeneralCategory.removePreference(mStatusBarBrightnessControl);
            } else {
                // Status bar brightness control
                mStatusBarBrightnessControl.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                        Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));
                try {
                    if (Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        mStatusBarBrightnessControl.setEnabled(false);
                        mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
                    }
                } catch (SettingNotFoundException e) {
                }
            }

            // Status bar double-tap to sleep
            mStatusBarDoubleTapSleepGesture = (CheckBoxPreference) getPreferenceScreen().findPreference(DOUBLE_TAP_SLEEP_GESTURE);
            mStatusBarDoubleTapSleepGesture.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.DOUBLE_TAP_SLEEP_GESTURE, 0) == 1));

            // Network traffic indicator
            mNetworkStats = (CheckBoxPreference) findPreference(NETWORK_STATS);
            mNetworkStats.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS, 0) == 1);
            mNetworkStats.setOnPreferenceChangeListener(this);

            mNetworkStatsUpdateFrequency = (SeekBarPreference)
                    findPreference(NETWORK_STATS_UPDATE_FREQUENCY);
            mNetworkStatsUpdateFrequency.setValue(Settings.System.getInt(getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, 500));
            mNetworkStatsUpdateFrequency.setOnPreferenceChangeListener(this);

            // Status bar battery style
            mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY);
            mStatusBarBattery.setOnPreferenceChangeListener(this);
            int batteryStyleValue = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, 0);
            mStatusBarBattery.setValue(String.valueOf(batteryStyleValue));
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());

            // Clock
            mStatusBarClockStyle = (ListPreference) findPreference(STATUS_BAR_CLOCK_STYLE);
            int statusBarClockStyle = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK_STYLE, 1);
            mStatusBarClockStyle.setValue(String.valueOf(statusBarClockStyle));
            mStatusBarClockStyle.setSummary(mStatusBarClockStyle.getEntry());
            mStatusBarClockStyle.setOnPreferenceChangeListener(this);
            
            // Am-Pm
            mStatusBarAmPm = (ListPreference) getPreferenceScreen().findPreference(STATUS_BAR_AM_PM);
            try {
                if (Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.TIME_12_24) == 24) {
                    mStatusBarAmPm.setEnabled(false);
                    mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
                }
            } catch (SettingNotFoundException e ) {
            }

            int statusBarAmPm = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_AM_PM, 2);
            mStatusBarAmPm.setValue(String.valueOf(statusBarAmPm));
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntry());
            mStatusBarAmPm.setOnPreferenceChangeListener(this);

            // Quick settings category
            // Quick Settings pull down
            mQuickPulldown = (ListPreference) getPreferenceScreen().findPreference(QUICK_PULLDOWN);
            // only show on phones
            if (!Utils.isPhone(getActivity())) {
                if (mQuickPulldown != null)
                    getPreferenceScreen().removePreference(mQuickPulldown);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(QUICK_SETTINGS_CATEGORY));
            } else {
                mQuickPulldown.setOnPreferenceChangeListener(this);
                int quickPulldownValue = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                        Settings.System.QS_QUICK_PULLDOWN, 0);
                mQuickPulldown.setValue(String.valueOf(quickPulldownValue));
                mQuickPulldown.setSummary(mQuickPulldown.getEntry());
            }
        }

    public boolean onPreferenceChange(Preference preference, Object objValue) {

        if (preference == mStatusBarBattery) {
            int batteryStyleValue = Integer.valueOf((String) objValue);
            int batteryStyleIndex = mStatusBarBattery.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, batteryStyleValue);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[batteryStyleIndex]);
            return true;

        } else if (preference == mNetworkStats) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), Settings.System.STATUS_BAR_NETWORK_STATS,
                    value ? 1 : 0);

        } else if (preference == mNetworkStatsUpdateFrequency) {
            int i = Integer.valueOf((Integer) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, i);
            return true;

        } else if (preference == mStatusBarClockStyle) {
            int statusBarClockStyle = Integer.valueOf((String) objValue);
            int index = mStatusBarClockStyle.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_CLOCK_STYLE, statusBarClockStyle);
            mStatusBarClockStyle.setSummary(mStatusBarClockStyle.getEntries()[index]);
            return true;

        } else if (preference == mStatusBarAmPm) {
            int statusBarAmPm = Integer.valueOf((String) objValue);
            int indexAmPm = mStatusBarAmPm.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_AM_PM, statusBarAmPm);
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntries()[indexAmPm]);
            return true;

        } else if (preference == mQuickPulldown) {
            int quickPulldownValue = Integer.valueOf((String) objValue);
            int quickPulldownIndex = mQuickPulldown.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_QUICK_PULLDOWN, quickPulldownValue);
            mQuickPulldown.setSummary(mQuickPulldown.getEntries()[quickPulldownIndex]);
            return true;

        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mStatusBarBrightnessControl) {
            value = mStatusBarBrightnessControl.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, value ? 1 : 0);
            return true;

        } else if (preference == mStatusBarDoubleTapSleepGesture) {
            value = mStatusBarDoubleTapSleepGesture.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.DOUBLE_TAP_SLEEP_GESTURE, value ? 1: 0);
            return true;

        }
        return false;
    }
}