package ca.mcmaster.potentiostat;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import ca.mcmaster.waterqualitymonitorsuite.R;


public class ExperimentActivity extends AppCompatActivity {
    private static final String TAG = ExperimentActivity.class.getSimpleName();
    List<Experiment> experimentList =  new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experiment);

        initExperimentList();

        //Set spinner items
        List<String> spinnerArray =  new ArrayList<>();
        initSpinnerArray(spinnerArray);


        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner sItems = (Spinner) findViewById(R.id.spinnerExperiment);
        sItems.setAdapter(adapter);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.title_activity_experiment);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            Log.e(TAG, "onCreate: Action support bar should not be null");
            finish();
        }

    }

    // TODO: 2017-11-30 update to grab saved experiments from file/preference
    private void initExperimentList(){
        experimentList.clear();
        Experiment exp1, exp2, exp3, exp4;
        exp1 = new Experiment("Verify Voltage Test Experiment 11/30", Experiment.EXP_DPV);
        exp1.setCmds(new String[]{"EA2 72 2 ","EG2 0 ","ED0 0 32768 32768 0 500 1 50 100 50 "});
        exp2 = new Experiment("acetaminophen(III), DPV", Experiment.EXP_CV);
        exp2.setCmds(new String[]{"EA2 72 2 ","EG2 0 ","EC0 0 32768 32768 100 -100 0 2 10 "});
        exp4 = new Experiment("potassium hexacyanoferrate(III), DPV", Experiment.EXP_CV);
        exp4.setCmds(new String[]{"EA2 72 2 ","EG2 0 ","ED0 0 32768 32768 0 500 1 50 100 50 "});
        exp3 = new Experiment("potassium hexacyanoferrate(III), CV", Experiment.EXP_CV);
        exp3.setCmds(new String[]{"EA2 72 2 ","EG2 0 ","EC0 0 32768 32768 100 -100 0 2 10 "});

        experimentList.add(exp1);
        experimentList.add(exp2);
        experimentList.add(exp3);
        experimentList.add(exp4);
    }

    private void initSpinnerArray(List<String> sList){
        sList.clear();
        sList.add("<none selected>");
        for (Experiment e:
             experimentList) {
            sList.add(e.getName());
        }


    }



}
