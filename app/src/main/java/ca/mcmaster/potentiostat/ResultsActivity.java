package ca.mcmaster.potentiostat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.mcmaster.waterqualitymonitorsuite.R;

public class ResultsActivity extends AppCompatActivity {
    private XYPlot plotPotResult;
    private SimpleXYSeries plotPotResultSeries;

    private static final String TAG = ResultsActivity.class.getSimpleName();

    private static final String DEF_DIR_PATH = "/WQM_Records";
    private static final String CV_FILE_NAME = "DemoCV.csv";
    private static final String DPV_FILE_NAME = "DemoDPV.csv";

    private String expName;
    private String expType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        //Get intent and extras
        final Intent intent = getIntent();
        expName = intent.getStringExtra(ExpSelectActivity.EXTRAS_EXP_NAME);
        expType = intent.getStringExtra(ExpSelectActivity.EXTRAS_EXP_TYPE);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.title_pot_results);
            getSupportActionBar().setSubtitle(expName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            Log.e(TAG, "onCreate: Action support bar should not be null");
            finish();
        }



        plotPotResult = (XYPlot) findViewById(R.id.plotPotResults);
        plotPotResult.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("0.000E0"));

        plotPotResultSeries = new SimpleXYSeries("Scan 1");
        LineAndPointFormatter formatter;
        if (expType.equals("CV"))
            formatter = new LineAndPointFormatter(null, Color.BLUE, null, null);
        else
            formatter = new LineAndPointFormatter(Color.BLUE, null, null, null);

        formatter.getVertexPaint().setStrokeWidth(8);
        plotPotResult.addSeries(plotPotResultSeries, formatter);

        //Demo file load and plot
        String fn = (expType.equals("CV")) ? CV_FILE_NAME : DPV_FILE_NAME;
        File fileR = new File(Environment.getExternalStoragePublicDirectory(DEF_DIR_PATH), fn);
        int numOfSeries = 1; // multiple series not needed for demo
        try {
            FileInputStream fis = new FileInputStream(fileR);
            DataInputStream in = new DataInputStream(fis);
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(in));
            plotPotResultSeries.clear();
            String strLine;
            while ((strLine = br.readLine()) != null) {
                List<String> strList = new ArrayList<String>(Arrays.asList(strLine.split(",")));
                // loop over each series
                String xStr, yStr = "";
                for (int i = 0; i < numOfSeries; i++) {
                    xStr = strList.get(2*i);
                    yStr = strList.get(2*i + 1);
                    if(!isNotNum(xStr)&& !isNotNum(yStr)) {
                        try {
                            //for (int j = 0; j < 1000; j++);
                            plotPotResultSeries.addLast(Double.parseDouble(xStr), Double.parseDouble(yStr));
                        } catch (Exception e) {
                        }
                    }
                }
            }
            in.close();
            //adjust domain boundaries and intervals
/*            plotPotResult.redraw();
            double min, max;
            min = plotPotResult.getBounds().getMinX().doubleValue();
            min = Math.floor(min/100.0) * 100;
            max = plotPotResult.getBounds().getMaxX().doubleValue();
            max = Math.ceil(max/100) * 100;
            plotPotResult.setDomainBoundaries(min,max,BoundaryMode.FIXED);
            plotPotResult.setDomainStep(StepMode.INCREMENT_BY_VAL, 100);*/
            plotPotResult.redraw();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    //Checks if string contains a space or is empty
    //Will return a string is not a number if it has space padding eg. "  99"
    private boolean isNotNum(String s){
        return (s.isEmpty()||s.contains(" "));
    }
}
