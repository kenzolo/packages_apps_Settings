/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.slim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import com.android.internal.widget.LockPatternUtils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.io.File;

public class LockscreenStyle extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "LockscreenStyle";

    private static final String KEY_LOCKSCREEN_COLORIZE_ICON =
            "lockscreen_colorize_icon";
    private static final String KEY_LOCKSCREEN_LOCK_ICON =
            "lockscreen_lock_icon";
    private static final String KEY_LOCKSCREEN_FRAME_COLOR =
            "lockscreen_frame_color";
    private static final String KEY_LOCKSCREEN_LOCK_COLOR =
            "lockscreen_lock_color";
    private static final String KEY_LOCKSCREEN_DOTS_COLOR =
            "lockscreen_dots_color";

    private String mDefault;

    private CheckBoxPreference mColorizeCustom;

    private ColorPickerPreference mFrameColor;
    private ColorPickerPreference mLockColor;
    private ColorPickerPreference mDotsColor;


    private boolean mCheckPreferences;

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        addPreferencesFromResource(R.xml.lockscreen_style);
        prefSet = getPreferenceScreen();

        // Set to string so we don't have to create multiple objects of it
        mDefault = getResources().getString(R.string.default_string);

        mFrameColor = (ColorPickerPreference)
                findPreference(KEY_LOCKSCREEN_FRAME_COLOR);
        mFrameColor.setOnPreferenceChangeListener(this);
        int frameColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_FRAME_COLOR, -2);
        setPreferenceSummary(mFrameColor,
                getResources().getString(
                R.string.lockscreen_frame_color_summary), frameColor);
        mFrameColor.setNewPreviewColor(frameColor);

        mLockColor = (ColorPickerPreference)
                findPreference(KEY_LOCKSCREEN_LOCK_COLOR);
        mLockColor.setOnPreferenceChangeListener(this);
        int lockColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_COLOR, -2);
        setPreferenceSummary(mLockColor,
                getResources().getString(
                R.string.lockscreen_lock_color_summary), lockColor);
        mLockColor.setNewPreviewColor(lockColor);

        mDotsColor = (ColorPickerPreference)
                findPreference(KEY_LOCKSCREEN_DOTS_COLOR);
        mDotsColor.setOnPreferenceChangeListener(this);
        int dotsColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_DOTS_COLOR, -2);
        setPreferenceSummary(mDotsColor,
                getResources().getString(
                R.string.lockscreen_dots_color_summary), dotsColor);
        mDotsColor.setNewPreviewColor(dotsColor);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefSet;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mFrameColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_FRAME_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_frame_color_summary), val);
            return true;
        } else if (preference == mLockColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_lock_color_summary), val);
            return true;
        } else if (preference == mDotsColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_DOTS_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_dots_color_summary), val);
            return true;
        }
        return false;
    }

    private void setPreferenceSummary(
            Preference preference, String defaultSummary, int value) {
        if (value == -2) {
            preference.setSummary(defaultSummary + " (" + mDefault + ")");
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & value));
            preference.setSummary(defaultSummary + " (" + hexColor + ")");
        }
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        LockscreenStyle getOwner() {
            return (LockscreenStyle) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.lockscreen_style_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.LOCKSCREEN_FRAME_COLOR, -2);
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.LOCKSCREEN_LOCK_COLOR, -2);
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.LOCKSCREEN_DOTS_COLOR, -2);
                            getOwner().createCustomView();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}
