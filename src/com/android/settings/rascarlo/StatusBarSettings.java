
package com.android.settings.rascarlo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.rascarlo.SeekBarPreference;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBarSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    // General
    private static String STATUS_BAR_GENERAL_CATEGORY = "status_bar_general_category";
    // Brightness control
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    // Status bar battery style
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    // Clock
    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock_style";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    // Quick Settings
    private static final String QUICK_SETTINGS_CATEGORY = "status_bar_quick_settings_category";
    private static final String QUICK_PULLDOWN = "quick_pulldown";
    // Network Stats
    private static final String STATUS_BAR_NETWORK_STATS = "status_bar_show_network_stats";
    private static final String NETWORK_STATS_UPDATE_FREQUENCY = "network_stats_update_frequency";
    private static final String STATUS_BAR_NETWORK_COLOR = "status_bar_network_color";
    private static final String STATUS_BAR_NETWORK_HIDE = "status_bar_network_hide";

    // General
    private PreferenceCategory mStatusBarGeneralCategory;
    // Status bar battery style
    private ListPreference mStatusBarBattery;
    // Brightness control
    private CheckBoxPreference mStatusBarBrightnessControl;
    // Clock
    private ListPreference mStatusBarAmPm;
    private ListPreference mStatusBarClockStyle;
    // Quick Settings
    private ListPreference mQuickPulldown;
    // Network Stats
    private SeekBarPreference mNetworkStatsUpdateFrequency;
    private CheckBoxPreference mStatusBarNetworkStats;
    private ColorPickerPreference mStatusBarNetworkColor;
    private CheckBoxPreference mStatusBarNetworkHide;

    private static final int MENU_RESET = Menu.FIRST;

    static final int DEFAULT_NETWORK_USAGE_COLOR = 0xffffffff;

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
            // Network Stats
            mStatusBarNetworkStats = (CheckBoxPreference) getPreferenceScreen().findPreference(STATUS_BAR_NETWORK_STATS);
            mStatusBarNetworkStats.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                     Settings.System.STATUS_BAR_NETWORK_STATS, 0) == 1));

            mNetworkStatsUpdateFrequency = (SeekBarPreference)
                    getPreferenceScreen().findPreference(NETWORK_STATS_UPDATE_FREQUENCY);
            mNetworkStatsUpdateFrequency.setValue((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, 500)));
            mNetworkStatsUpdateFrequency.setOnPreferenceChangeListener(this);

            // custom colors
            mStatusBarNetworkColor = (ColorPickerPreference) getPreferenceScreen().findPreference(STATUS_BAR_NETWORK_COLOR);
            mStatusBarNetworkColor.setOnPreferenceChangeListener(this);
            int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                       Settings.System.STATUS_BAR_NETWORK_COLOR, 0xff000000);
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mStatusBarNetworkColor.setSummary(hexColor);
            mStatusBarNetworkColor.setNewPreviewColor(intColor);

            // hide if there's no traffic
            mStatusBarNetworkHide = (CheckBoxPreference) getPreferenceScreen().findPreference(STATUS_BAR_NETWORK_HIDE);
            mStatusBarNetworkHide.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_HIDE, 0) == 1));

            setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.status_bar_network_usage_color_reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.status_bar_network_usage_color_reset);
        alertDialog.setMessage(R.string.status_bar_network_usage_color_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                NetworkStatsColorReset();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void NetworkStatsColorReset() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_COLOR, DEFAULT_NETWORK_USAGE_COLOR);
        
        mStatusBarNetworkColor.setNewPreviewColor(DEFAULT_NETWORK_USAGE_COLOR);
        String hexColor = String.format("#%08x", (0xffffffff & DEFAULT_NETWORK_USAGE_COLOR));
        mStatusBarNetworkColor.setSummary(hexColor);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {

        if (preference == mStatusBarBattery) {
            int batteryStyleValue = Integer.valueOf((String) objValue);
            int batteryStyleIndex = mStatusBarBattery.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, batteryStyleValue);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[batteryStyleIndex]);
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

        } else if (preference == mNetworkStatsUpdateFrequency) {
            int i = Integer.valueOf((Integer) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, i);
            return true;

        } else if (preference == mStatusBarNetworkColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_COLOR, intHex);
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

        } else if (preference == mStatusBarNetworkStats) {
            value = mStatusBarNetworkStats.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS, value ? 1 : 0);
            return true;

        } else if (preference == mStatusBarNetworkHide) {
            value = mStatusBarNetworkHide.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_HIDE, value ? 1 : 0);
            return true;
        }
        return false;
    }
}
