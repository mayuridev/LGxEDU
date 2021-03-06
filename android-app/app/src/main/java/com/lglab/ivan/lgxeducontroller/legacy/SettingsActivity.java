package com.lglab.ivan.lgxeducontroller.legacy;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.EditText;

import com.lglab.ivan.lgxeducontroller.R;
import com.lglab.ivan.lgxeducontroller.connection.LGApi;
import com.lglab.ivan.lgxeducontroller.connection.LGConnectionManager;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */


public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.za
        bindPreferenceSummaryToValue(findPreference("SSH-USER"));
        bindPreferenceSummaryToValue(findPreference("SSH-PASSWORD"));
        bindPreferenceSummaryToValue(findPreference("SSH-IP"));
        bindPreferenceSummaryToValue(findPreference("SSH-PORT"));

        bindPreferenceSummaryToValue(findPreference("isOnChromeBook"));
        bindPreferenceSummaryToValue(findPreference("KML-API-IP"));
        bindPreferenceSummaryToValue(findPreference("KML-API-PORT"));


        bindPreferenceSummaryToValue(findPreference("AdminPassword"));
        bindPreferenceSummaryToValue(findPreference("pref_kiosk_mode"));
        bindPreferenceSummaryToValue(findPreference("ServerIp"));
        bindPreferenceSummaryToValue(findPreference("ServerPort"));
    }

    public void onStop() {
        super.onStop();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        LGConnectionManager.getInstance().setData(prefs.getString("SSH-USER", "lg"), prefs.getString("SSH-PASSWORD", "lqgalaxy"), prefs.getString("SSH-IP", "192.168.86.39"), Integer.parseInt(prefs.getString("SSH-PORT", "22")));

        LGApi.SERVER_IP = prefs.getString("KML-API-IP", "192.168.86.145");
        LGApi.PORT = Integer.parseInt(prefs.getString("KML-API-PORT", "8112"));
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);


        // Trigger the listener immediately with the preference's
        // current value.
        if (preference.getKey().equals("pref_kiosk_mode") || preference.getKey().equals("isOnChromeBook")) {
            onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getBoolean(preference.getKey(), false));
        } else {
            onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (preference.getKey().toLowerCase().contains("password")) {
            EditText edit = ((EditTextPreference) preference).getEditText();
            String pref = edit.getTransformationMethod().getTransformation(stringValue, edit).toString();
            preference.setSummary(pref);

        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }

        return true;
    }

    /**
     * Això és per a que un cop entrem a settings, al tornar enrere(osigui a LGPCActivity) continui tal qual estava, és a dir,
     * que el contingut del DetailFragment sigui el mateix que abans i no estigui en blanc.
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
