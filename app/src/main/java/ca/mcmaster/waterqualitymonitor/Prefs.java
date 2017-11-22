package ca.mcmaster.waterqualitymonitor;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

/**
 * Created by DK on 2017-10-31.
 */

public class Prefs extends AppCompatActivity {
    public final static String TAG = Prefs.class.getSimpleName();

    public final static String DEF_SAMPLES = "50";
    public final static String DEF_AVERAGE = "10";
    public final static String DEF_ECAL = "280";

    private PF prefFrag; //preference fragment

    SharedPreferences prefs;
    SharedPreferences.OnSharedPreferenceChangeListener spChanged;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        prefFrag = new PF();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                prefFrag).commit();

        spChanged = new
                SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                          String key) {
                        verifyIntPreferences(sharedPreferences);
                        //Notify MeasurementActivity if certain settings updated
                        if (key.equals("pref_average")||key.equals("pref_samples")||key.equals("pref_cal_ph7"))
                            setResult(MeasurementActivity.REQUEST_CODE_NOTIFY_IF_UPDATED);
                    }
                };
        prefs.registerOnSharedPreferenceChangeListener(spChanged);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*Verifies preferences (String values) that should be integer values can
    be converted (parsed) to integer values. Assigns default values
    if an exception occurs. Also checks converted integers are in a valid range*/
    private void verifyIntPreferences(SharedPreferences sp){
        int samples, average;
        //Max Samples
        try {
            samples = Integer.parseInt(sp.getString("pref_samples",null));
            //Check range
            if (samples < 1){
                //sp.edit().putString("pref_samples","1").apply();
                samples = 1;
            }
            if (samples > 1000){
                //sp.edit().putString("pref_samples","1000").apply();
                samples = 1000;
            }
        } catch (NumberFormatException e){
            //sp.edit().putString("pref_samples",DEF_SAMPLES).apply();
            samples = Integer.parseInt(DEF_SAMPLES);
        }
        //Average
        try {
            average = Integer.parseInt(sp.getString("pref_average",null));
            //Check range
            if (average < 1){
                //sp.edit().putString("pref_average","1").apply();
                average = 1;
            }
            if (average > samples){
                //sp.edit().putString("pref_average",String.valueOf(samples)).apply();
                average = samples;
            }
        } catch (NumberFormatException e){
            //sp.edit().putString("pref_average",DEF_AVERAGE).apply();
            average = Integer.parseInt(DEF_AVERAGE);

        }
        try {
            prefFrag.etextAverage.setText(String.valueOf(average));
            prefFrag.etextSamples.setText(String.valueOf(samples));
        } catch (Exception e) {
            Log.e(TAG, "verifyIntPreferences: Exception occurred: " +e.toString());
            e.printStackTrace();
        }
    }

}
