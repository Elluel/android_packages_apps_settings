/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.print;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.print.PrintManager;
import android.print.PrinterDiscoverySession;
import android.print.PrinterDiscoverySession.OnPrintersChangeListener;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.print.PrintSettingsFragment.ToggleSwitch;
import com.android.settings.print.PrintSettingsFragment.ToggleSwitch.OnBeforeCheckedChangeListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment with print service settings.
 */
public class PrintServiceSettingsFragment extends SettingsPreferenceFragment
        implements DialogInterface.OnClickListener {

    private static final int LOADER_ID_PRINTERS_LOADER = 1;

    private static final int DIALOG_ID_ENABLE_WARNING = 1;

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            List<ComponentName> services = SettingsUtils.readEnabledPrintServices(getActivity());
            final boolean enabled = services.contains(mComponentName);
            mToggleSwitch.setCheckedInternal(enabled);
        }
    };

    protected ToggleSwitch mToggleSwitch;

    protected String mPreferenceKey;

    protected CharSequence mSettingsTitle;
    protected Intent mSettingsIntent;

    protected CharSequence mAddPrintersTitle;
    protected Intent mAddPrintersIntent;

    private CharSequence mEnableWarningTitle;
    private CharSequence mEnableWarningMessage;

    private ComponentName mComponentName;

    // TODO: Showing sub-sub fragment does not handle the activity title
    // so we do it but this is wrong. Do a real fix when there is time.
    private CharSequence mOldActivityTitle;

    @Override
    public void onResume() {
        mSettingsContentObserver.register(getContentResolver());
        super.onResume();
    }

    @Override
    public void onPause() {
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onInstallActionBarToggleSwitch();
        processArguments(getArguments());
        getListView().setAdapter(new PrintersAdapter());
    }

    @Override
    public void onDestroyView() {
        getActivity().getActionBar().setCustomView(null);
        if (mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(mOldActivityTitle);
        }
        mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        super.onDestroyView();
    }

    private void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ComponentName service = ComponentName.unflattenFromString(preferenceKey);
        List<ComponentName> services = SettingsUtils.readEnabledPrintServices(getActivity());
        if (enabled) {
            services.add(service);
        } else {
            services.remove(service);
        }
        SettingsUtils.writeEnabledPrintServices(getActivity(), services);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        CharSequence title = null;
        CharSequence message = null;
        switch (dialogId) {
            case DIALOG_ID_ENABLE_WARNING:
                title = mEnableWarningTitle;
                message = mEnableWarningMessage;
                break;
            default:
                throw new IllegalArgumentException();
        }
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final boolean checked;
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                checked = true;
                mToggleSwitch.setCheckedInternal(checked);
                getArguments().putBoolean(PrintSettingsFragment.EXTRA_CHECKED, checked);
                onPreferenceToggled(mPreferenceKey, checked);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                checked = false;
                mToggleSwitch.setCheckedInternal(checked);
                getArguments().putBoolean(PrintSettingsFragment.EXTRA_CHECKED, checked);
                onPreferenceToggled(mPreferenceKey, checked);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    protected void onInstallActionBarToggleSwitch() {
        mToggleSwitch = createAndAddActionBarToggleSwitch(getActivity());
        mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                if (checked) {
                    if (!TextUtils.isEmpty(mEnableWarningMessage)) {
                        toggleSwitch.setCheckedInternal(false);
                        getArguments().putBoolean(PrintSettingsFragment.EXTRA_CHECKED, false);
                        showDialog(DIALOG_ID_ENABLE_WARNING);
                        return true;
                    }
                    onPreferenceToggled(mPreferenceKey, true);
                } else {
                    onPreferenceToggled(mPreferenceKey, false);
                }
                return false;
            }
        });
    }

    private void processArguments(Bundle arguments) {
        // Key.
        mPreferenceKey = arguments.getString(PrintSettingsFragment.EXTRA_PREFERENCE_KEY);

        // Enabled.
        final boolean enabled = arguments.getBoolean(PrintSettingsFragment.EXTRA_CHECKED);
        mToggleSwitch.setCheckedInternal(enabled);

        // Title.
        PreferenceActivity activity = (PreferenceActivity) getActivity();
        if (!activity.onIsMultiPane() || activity.onIsHidingHeaders()) {
            mOldActivityTitle = getActivity().getTitle();
            String title = arguments.getString(PrintSettingsFragment.EXTRA_TITLE);
            getActivity().getActionBar().setTitle(title);
        }

        // Settings title and intent.
        String settingsTitle = arguments.getString(PrintSettingsFragment.EXTRA_SETTINGS_TITLE);
        String settingsComponentName = arguments.getString(
                PrintSettingsFragment.EXTRA_SETTINGS_COMPONENT_NAME);
        if (!TextUtils.isEmpty(settingsTitle) && !TextUtils.isEmpty(settingsComponentName)) {
            Intent settingsIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                    ComponentName.unflattenFromString(settingsComponentName.toString()));
            if (!getPackageManager().queryIntentActivities(settingsIntent, 0).isEmpty()) {
                mSettingsTitle = settingsTitle;
                mSettingsIntent = settingsIntent;
                setHasOptionsMenu(true);
            }
        }

        // Add printers title and intent.
        String addPrintersTitle = arguments.getString(
                PrintSettingsFragment.EXTRA_ADD_PRINTERS_TITLE);
        String addPrintersComponentName =
                arguments.getString(PrintSettingsFragment.EXTRA_ADD_PRINTERS_COMPONENT_NAME);
        if (!TextUtils.isEmpty(addPrintersTitle)
                && !TextUtils.isEmpty(addPrintersComponentName)) {
            Intent addPritnersIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                    ComponentName.unflattenFromString(addPrintersComponentName.toString()));
            if (!getPackageManager().queryIntentActivities(addPritnersIntent, 0).isEmpty()) {
                mAddPrintersTitle = addPrintersTitle;
                mAddPrintersIntent = addPritnersIntent;
                setHasOptionsMenu(true);
            }
        }

        // Enable warning title.
        mEnableWarningTitle = arguments.getCharSequence(
                PrintSettingsFragment.EXTRA_ENABLE_WARNING_TITLE);

        // Enable warning message.
        mEnableWarningMessage = arguments.getCharSequence(
                PrintSettingsFragment.EXTRA_ENABLE_WARNING_MESSAGE);

        // Component name.
        mComponentName = ComponentName.unflattenFromString(arguments
                .getString(PrintSettingsFragment.EXTRA_SERVICE_COMPONENT_NAME));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.print_service_settings, menu);

        MenuItem addPrinters = menu.findItem(R.id.print_menu_item_add_printer);
        if (!TextUtils.isEmpty(mAddPrintersTitle) && mAddPrintersIntent != null) {
            addPrinters.setIntent(mAddPrintersIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_add_printer);
        }

        MenuItem settings = menu.findItem(R.id.print_menu_item_settings);
        if (!TextUtils.isEmpty(mSettingsTitle) && mSettingsIntent != null) {
            settings.setIntent(mSettingsIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_settings);
        }

        MenuItem uninstall = menu.findItem(R.id.print_menu_item_uninstall);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE,
                Uri.parse("package:" + mComponentName.getPackageName()));
        uninstall.setIntent(uninstallIntent);
    }

    private ToggleSwitch createAndAddActionBarToggleSwitch(Activity activity) {
        ToggleSwitch toggleSwitch = new ToggleSwitch(activity);
        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        toggleSwitch.setPaddingRelative(0, 0, padding, 0);
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(toggleSwitch,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
        return toggleSwitch;
    }

    private static abstract class SettingsContentObserver extends ContentObserver {

    public SettingsContentObserver(Handler handler) {
        super(handler);
    }

    public void register(ContentResolver contentResolver) {
        contentResolver.registerContentObserver(Settings.Secure.getUriFor(
                Settings.Secure.ENABLED_PRINT_SERVICES), false, this);
    }

    public void unregister(ContentResolver contentResolver) {
        contentResolver.unregisterContentObserver(this);
    }

    @Override
    public abstract void onChange(boolean selfChange, Uri uri);

    }

    private final class PrintersAdapter extends BaseAdapter
            implements LoaderManager.LoaderCallbacks<List<PrinterInfo>>{
        private final List<PrinterInfo> mPrinters = new ArrayList<PrinterInfo>();

        public PrintersAdapter() {
            getLoaderManager().initLoader(LOADER_ID_PRINTERS_LOADER, null, this);
        }

        @Override
        public int getCount() {
            return mPrinters.size();
        }

        @Override
        public Object getItem(int position) {
            return mPrinters.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(
                        R.layout.preference, parent, false);
            }

            PrinterInfo printer = (PrinterInfo) getItem(position);
            CharSequence title = printer.getName();
            CharSequence subtitle = null;
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(
                        printer.getId().getServiceName().getPackageName(), 0);
                        subtitle = packageInfo.applicationInfo.loadLabel(getPackageManager());
            } catch (NameNotFoundException nnfe) {
                /* ignore */
            }

            TextView titleView = (TextView) convertView.findViewById(android.R.id.title);
            titleView.setText(title);

            TextView subtitleView = (TextView) convertView.findViewById(android.R.id.summary);
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(View.VISIBLE);
            } else {
                subtitleView.setText(null);
                subtitleView.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override
        public Loader<List<PrinterInfo>> onCreateLoader(int id, Bundle args) {
            if (id == LOADER_ID_PRINTERS_LOADER) {
                return new PrintersLoader(getActivity());
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<List<PrinterInfo>> loader,
                List<PrinterInfo> printers) {
            mPrinters.clear();
            final int printerCount = printers.size();
            for (int i = 0; i < printerCount; i++) {
                PrinterInfo printer = printers.get(i);
                if (printer.getId().getServiceName().equals(mComponentName)) {
                    mPrinters.add(printer);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<PrinterInfo>> loader) {
            mPrinters.clear();
            notifyDataSetInvalidated();
        }
    }

    private static class PrintersLoader extends Loader<List<PrinterInfo>> {

        private static final String LOG_TAG = "PrintersLoader";

        private static final boolean DEBUG = true && Build.IS_DEBUGGABLE;

        private final Map<PrinterId, PrinterInfo> mPrinters =
                new LinkedHashMap<PrinterId, PrinterInfo>();

        private PrinterDiscoverySession mDiscoverySession;

        public PrintersLoader(Context context) {
            super(context);
        }

        @Override
        public void deliverResult(List<PrinterInfo> printers) {
            if (isStarted()) {
                super.deliverResult(printers);
            }
        }

        @Override
        protected void onStartLoading() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onStartLoading()");
            }
            // The contract is that if we already have a valid,
            // result the we have to deliver it immediately.
            if (!mPrinters.isEmpty()) {
                deliverResult(new ArrayList<PrinterInfo>(mPrinters.values()));
            }
            // We want to start discovery at this point.
            onForceLoad();
        }

        @Override
        protected void onStopLoading() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onStopLoading()");
            }
            onCancelLoad();
        }

        @Override
        protected void onForceLoad() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onForceLoad()");
            }
            loadInternal();
        }

        @Override
        protected boolean onCancelLoad() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onCancelLoad()");
            }
            return cancelInternal();
        }

        @Override
        protected void onReset() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onReset()");
            }
            onStopLoading();
            mPrinters.clear();
            if (mDiscoverySession != null) {
                mDiscoverySession.destroy();
                mDiscoverySession = null;
            }
        }

        @Override
        protected void onAbandon() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onAbandon()");
            }
            onStopLoading();
        }

        private boolean cancelInternal() {
            if (mDiscoverySession != null
                    && mDiscoverySession.isPrinterDiscoveryStarted()) {
                mDiscoverySession.stopPrinterDiscovery();
                return true;
            }
            return false;
        }

        private void loadInternal() {
            if (mDiscoverySession == null) {
                PrintManager printManager = (PrintManager) getContext()
                        .getSystemService(Context.PRINT_SERVICE);
                mDiscoverySession = printManager.createPrinterDiscoverySession();
                mDiscoverySession.setOnPrintersChangeListener(new OnPrintersChangeListener() {
                    @Override
                    public void onPrintersChanged() {
                        deliverResult(new ArrayList<PrinterInfo>(
                                mDiscoverySession.getPrinters()));
                    }
                });
            }
            mDiscoverySession.startPrinterDisovery(null);
        }
    }
}
