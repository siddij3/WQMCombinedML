package ca.mcmaster.waterqualitymonitorsuite;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Created by DK on 2017-11-03.
 */

public class PF extends PreferenceFragment
{
    public EditTextPreference etextSamples;
    public EditTextPreference etextAverage;
    public EditTextPreference etextEcal;
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        etextAverage = (EditTextPreference) findPreference("pref_average");
        etextSamples = (EditTextPreference) findPreference("pref_samples");
        etextEcal = (EditTextPreference) findPreference("pref_cal_ph7");

        //"button" to reset Ecal to default value, currently not implemented
        /*
        Preference button = findPreference("reset_ecal");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                etextEcal.setText(Prefs.DEF_PHCALOFFSET);
                return true;
            }
        });*/

    }

}
