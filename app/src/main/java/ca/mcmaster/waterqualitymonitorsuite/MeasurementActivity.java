package ca.mcmaster.waterqualitymonitorsuite;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import android.location.Location;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;

import java.util.*;

import ca.mcmaster.testsuitecommon.BluetoothLeService;
import ca.mcmaster.testsuitecommon.DeviceScanActivity;
import ca.mcmaster.testsuitecommon.GattAttributes;

public class MeasurementActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MeasurementActivity.class.getSimpleName();


    private boolean demoMode = false;

    private static final int VIEW_MEAS = 0;
    private static final int VIEW_CAL = 1;
    private static final boolean INCLUDE_STATS = false;

    private static final double CL_MEAS_THRESHOLD = 70.0;
    private static final double ALK_MEAS_THRESHOLD = 70.0; //TODO Find threshold for alkalinity


    public static final int MAX_READ_LINE_LEN = 50; //Maximum expected size in chars per msg
    private static final String TITLE_HEADER = "Water Quality Monitor Results"; //Exported file header title

    //Request Codes
    private static final int REQUEST_CODE_SAVE_TO_DRIVE = 1;
    public final static int REQUEST_CODE_NOTIFY_IF_UPDATED = 2; //Request code for starting Prefs activity

    //Demo Data
    //pH data in mV
    private static final double[] DEMO_PH_E = {499.767,494.1002,488.4335,484.8969,481.3604,479.1831,477.0058,475.5385,474.0713,473.2279,472.3846,471.5623,470.74,469.6049,468.4698,467.9539,467.4381,467.0957,466.7533,466.3692,465.9851,465.7744,465.5636,465.3774,465.1912,464.9209,464.6506,464.4131,464.1756,463.9541,463.7327,463.6017,463.4708,463.3505,463.2302,463.1743,463.1185,463.0234,462.9283,462.9125,462.8968,462.8779,462.859,462.8424,462.8257,462.8082,462.7907,462.7723,462.7539,462.7413,462.7287,462.716,462.7033,462.6885,462.6737,462.6591,462.6445,462.6355,462.6266,462.6113,462.6113};
    //Cl data in nA
    private static final double[] DEMO_CL_I = {-3218.89,-2281.19,-2218.72,-2156.26,-1906.39,-1728.15,-1653.15,-1584.41,-1528.14,-1471.9,-1443.46,-1402.83,-1365.96,-1333.46,-1303.46,-1274.71,-1248.46,-1224.71,-1202.83,-1179.71,-1159.08,-1139.08,-1120.95,-1105.33,-1089.7,-1074.7,-1060.33,-1046.58,-1032.83,-1020.33,-1008.45,-997.828,-986.577,-975.327,-963.452,-952.202,-944.703,-935.951,-927.827,-919.076,-911.577,-903.451,-895.326,-886.576,-880.326,-873.612,-867.2389,-861.2067,-855.5155,-850.1652,-845.1558,-840.4874,-836.1598,-832.1733,-828.5276,-825.2229,-822.2591,-819.6362,-817.3543,-815.4133,-815.4133};
    private static final double[] DEMO_ALK_I = {-3218.89,-2281.19,-2218.72,-2156.26,-1906.39,-1728.15,-1653.15,-1584.41,-1528.14,-1471.9,-1443.46,-1402.83,-1365.96,-1333.46,-1303.46,-1274.71,-1248.46,-1224.71,-1202.83,-1179.71,-1159.08,-1139.08,-1120.95,-1105.33,-1089.7,-1074.7,-1060.33,-1046.58,-1032.83,-1020.33,-1008.45,-997.828,-986.577,-975.327,-963.452,-952.202,-944.703,-935.951,-927.827,-919.076,-911.577,-903.451,-895.326,-886.576,-880.326,-873.612,-867.2389,-861.2067,-855.5155,-850.1652,-845.1558,-840.4874,-836.1598,-832.1733,-828.5276,-825.2229,-822.2591,-819.6362,-817.3543,-815.4133,-815.4133};

    private int activeView;
    private SharedPreferences sharedPrefs;

    private boolean ValidClRecorded; //atleast one valid Cl measurement obtained since starting measurement
    private boolean ValidAlkRecorded; //atleast one valid Cl measurement obtained since starting measurement
    private boolean calpHViewActive;
    private boolean measuring;
    private int readFails;

    private String deviceName;
    private String deviceAddress;

    private BluetoothLeService bluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> gattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean BLE_Connected = false;
    private boolean enableWrite = false;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private BluetoothGattCharacteristic writeCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    CalcLinearRegression calcLR;

    //GUI elements
    private Button btnCal_pH7;
    private Button btnCal_pH4;
    private Button btnCal_pH10;
    private Button btnCal_tLo;
    private Button btnCal_tHi;
    private View viewMeas;
    private View viewCal;
    private View viewCalAdvDisplay;
    private View viewCalStdDisplay;

    private View viewButtons_pH;
    private View viewButtons_T;

    private TextView[] tvCurrentVals = new TextView[8];
    private TextView[] tvAvgVals = new TextView[9];
    private TextView[] tvStats = new TextView[7]; // TODO may have to add more here

    private TextView[] tvCalpHLbl = new TextView[3];
    private TextView[] tvCalTLbl = new TextView[3];

    private StringBuilder sbRead = new StringBuilder();

    //Plot Variables
    private XYPlot calPlot;
    private XYSeries calPlotSeries;

    private XYPlot measPlot_T;
    private XYPlot measPlot_Cl;
    private XYPlot measPlot_pH;
    private XYPlot measPlot_alk;


    private XYSeries measPlotSeries_T;
    private XYSeries measPlotSeries_Cl;
    private XYSeries measPlotSeries_pH;
    private XYSeries measPlotSeries_alk;

    //Array list for storing measurements
    private List<MeasData> measList = new ArrayList<>();


    //Location Services
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    //Stored mean voltage and status for calibration
    double averageCalVoltage_pH;
    double averageCalVoltage_t;
    double averageCalVoltage_alk;
    boolean averageCalVoltageValid;

    //Preference/Settings display
    private TextView tvSampSize;
    private TextView tvAvgSize;
    private TextView tvEcal;
    private TextView tvSamples;

    //Values from preferences
    private int maxSampleSize;
    private int avgSampleSize;
    private boolean displayAdvCal;
    private double phCalOffset;
    private double phCalSlopeLo;
    private double phCalV4;
    private double phCalSlopeHi;
    private double phCalV10;
    private double tCalV100;
    private double tCalOffset;
    private double tCalSlope;

    private double ClCalSlope;
    private double ClCalOffset;
    private double ClCalLevel; // free Cl ppm corresponding to Cl Offset value

    // Temp in the event that alkalinity measurements needs more
    private double alkCalSlope;
    private double alkCalOffset;
    private double alkCalLevel; // free Cl ppm corresponding to Cl Offset value


    //Timer functionality, used only for demo mode to simulate samples
    long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;


            Random rand = new Random();

            double t, e, i, alk;
            double m, x, b;

            //generate ramp up, ramp down, and flat section
            /*if ((seconds >= 0) && (seconds < 15)){
                m = 1.0;
                x = seconds - 0;
                b = 0;
            } else if ((seconds >= 15) && (seconds < 30)){
                m = -1.0;
                x = seconds - 10;
                b = 15.0;
            } else {
                m = 0.0;
                x = seconds - 20;
                b = 0;
            }*/

            t = 436.9 + 0.06*(rand.nextDouble()*2-1);
            e = DEMO_PH_E[seconds];
            i = DEMO_CL_I[seconds];
            alk = DEMO_ALK_I[seconds];
            //e = -50.0 + 0.02*(m*x+b) + 0.03*(rand.nextDouble()*2-1);
            //i = 500.0 + 0.04*(m*x+b) + 0.06*(rand.nextDouble()*2-1);

            updateDataSwClAlk(t, e,  i, alk, (double)seconds, seconds>50);
            timerHandler.postDelayed(this, 1000);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.title_wqm);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            Log.e(TAG, "onCreate: Action support bar should not be null");
            finish();
        }


        //Intent and extra data
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(DeviceScanActivity.EXTRAS_DEVICE_NAME);
        deviceAddress = intent.getStringExtra(DeviceScanActivity.EXTRAS_DEVICE_ADDRESS);

        tvCurrentVals[0] = (TextView) findViewById(R.id.val_currentTemp);
        tvCurrentVals[1] = (TextView) findViewById(R.id.val_currentPh);
        tvCurrentVals[2] = (TextView) findViewById(R.id.val_currentCl);
        tvCurrentVals[3] = (TextView) findViewById(R.id.val_currentAlk);
        tvCurrentVals[4] = (TextView) findViewById(R.id.val_TempRaw);
        tvCurrentVals[5] = (TextView) findViewById(R.id.val_PhRaw);
        tvCurrentVals[6] = (TextView) findViewById(R.id.val_ClRaw);
        tvCurrentVals[7] = (TextView) findViewById(R.id.val_AlkRaw);

        tvAvgVals[0] = (TextView) findViewById(R.id.val_avgTemp);
        tvAvgVals[1] = (TextView) findViewById(R.id.val_avgPh);
        tvAvgVals[2] = (TextView) findViewById(R.id.val_avgCl);
        tvAvgVals[3] = (TextView) findViewById(R.id.val_avgAlk);

        tvAvgVals[4] = (TextView) findViewById(R.id.val_avgTRaw_Adv);
        tvAvgVals[5] = (TextView) findViewById(R.id.val_avgTRaw);
        tvAvgVals[6] = (TextView) findViewById(R.id.val_avgPhRaw_Adv);
        tvAvgVals[7] = (TextView) findViewById(R.id.val_avgPhRaw);


        tvSamples = (TextView) findViewById(R.id.val_Samples);

        tvSampSize  = (TextView) findViewById(R.id.val_samp_size);
        tvAvgSize  = (TextView) findViewById(R.id.val_avg_size);
        tvEcal  = (TextView) findViewById(R.id.val_ecal);

        tvStats[0]  = (TextView) findViewById(R.id.val_slope);
        tvStats[1]  = new TextView(this); //place holder for intercept, not used currently
        tvStats[2]  = (TextView) findViewById(R.id.val_r2);
        tvStats[3]  = (TextView) findViewById(R.id.val_StdDev);
        tvStats[4]  = (TextView) findViewById(R.id.val_scoreAdv);
        tvStats[5]  = (TextView) findViewById(R.id.val_score);
        tvStats[6]  = (TextView) findViewById(R.id.val_PkPk);

        //Set calibration values from stored preferences
        getValsFromPrefs();

        //Update Settings display with values from preferences
        updateSettingsDisplay();

        //Class for regression calculations
        calcLR = new CalcLinearRegression(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        viewCal = findViewById(R.id.layoutCal);
        viewCalAdvDisplay = findViewById(R.id.layoutAdvCal);
        viewCalStdDisplay = findViewById(R.id.layoutStdCal);
        viewMeas = findViewById(R.id.layoutMeas);

        //Views for toggling visibility between T and pH calibration
        tvCalpHLbl[0] = findViewById(R.id.lblStats_pH);
        tvCalpHLbl[1] = findViewById(R.id.lblavgpH);
        tvCalpHLbl[2] = findViewById(R.id.lblavgpH_Adv);
        tvCalTLbl[0] = findViewById(R.id.lblStats_T);
        tvCalTLbl[1] = findViewById(R.id.lblavgRawT);
        tvCalTLbl[2] = findViewById(R.id.lblavgRawT_Adv);
        //TODO is it needed to add alkalinity calibration?

        viewButtons_pH = findViewById(R.id.calButtons_pH);
        viewButtons_T = findViewById(R.id.calButtons_T);
        setView(VIEW_MEAS);
        startMeasuring(true);
        calpHViewActive = true;

        readFails = 0;

        //Location Services using google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //XY Plots
        calPlot = (XYPlot) findViewById(R.id.calPlot);
        calPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#.00"));
        calPlotSeries = new SimpleXYSeries("Cal. voltage (mV)");
        calPlot.addSeries(calPlotSeries, new LineAndPointFormatter(Color.BLUE, null, null, null));

        measPlot_T = (XYPlot) findViewById(R.id.measPlot_T);
        measPlot_T.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#.00"));
        measPlot_T.setRangeBoundaries(20, 21.8, BoundaryMode.FIXED );
        measPlotSeries_T = new SimpleXYSeries("Temperature");
        measPlot_T.addSeries(measPlotSeries_T, new LineAndPointFormatter(Color.DKGRAY, null, null, null));

        measPlot_pH = (XYPlot) findViewById(R.id.measPlot_pH);
        measPlot_pH.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#.00"));
        measPlotSeries_pH = new SimpleXYSeries("pH Level");
        measPlot_pH.addSeries(measPlotSeries_pH, new LineAndPointFormatter(Color.BLUE, null, null, null));

        measPlot_Cl = (XYPlot) findViewById(R.id.measPlot_Cl);
        measPlot_Cl.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#.00"));
        measPlotSeries_Cl = new SimpleXYSeries("Free Cl");
        measPlot_Cl.addSeries(measPlotSeries_Cl, new LineAndPointFormatter(Color.RED, null, null, null));

        measPlot_alk = (XYPlot) findViewById(R.id.measPlot_Alk);
        measPlot_alk.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#.00"));
        measPlotSeries_alk = new SimpleXYSeries("Alkalinity");
        measPlot_alk.addSeries(measPlotSeries_alk, new LineAndPointFormatter(Color.GREEN, null, null, null));


        //pH7 calibrate button and click listener
        btnCal_pH7 = findViewById(R.id.btnCal_pH7);
        btnCal_pH7.setVisibility(View.INVISIBLE);
        btnCal_pH7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (averageCalVoltageValid) {
                    new AlertDialog.Builder(MeasurementActivity.this)
                            .setTitle("Update Calibration Voltage")
                            .setMessage("Do you really want to update the calibration voltage for pH = 7 with the current average pH potential?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //User clicked ok, reset calibration voltage
                                    if(averageCalVoltageValid) {
                                        phCalOffset = averageCalVoltage_pH;
                                        sharedPrefs.edit().putString("pref_cal_ph7",String.valueOf(phCalOffset)).apply();
                                    }
                                    updateSettingsDisplay();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            }
        });

        //pH4 calibrate button and click listener
        btnCal_pH4 = findViewById(R.id.btnCal_pH4);
        btnCal_pH4.setVisibility(View.INVISIBLE);
        btnCal_pH4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (averageCalVoltageValid) {
                    new AlertDialog.Builder(MeasurementActivity.this)
                            .setTitle("Update Calibration Voltage")
                            .setMessage("Do you really want to update the calibration voltage for pH = 4 with the current average pH potential?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //User clicked ok, reset calibration voltage
                                    if(averageCalVoltageValid) {
                                        phCalV4 = averageCalVoltage_pH;
                                        sharedPrefs.edit().putString(Prefs.PREF_PH4_VAL,String.valueOf(phCalV4)).apply();
                                        phCalSlopeLo = (phCalOffset - phCalV4) / 3;
                                        sharedPrefs.edit().putString("pref_cal_phslopelo",String.valueOf(phCalSlopeLo)).apply();
                                    }
                                    updateSettingsDisplay();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            }
        });

        //pH10 calibrate button and click listener
        btnCal_pH10 = findViewById(R.id.btnCal_pH10);
        btnCal_pH10.setVisibility(View.INVISIBLE);
        btnCal_pH10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (averageCalVoltageValid) {
                    new AlertDialog.Builder(MeasurementActivity.this)
                            .setTitle("Update Calibration Voltage")
                            .setMessage("Do you really want to update the calibration voltage for pH = 10 with the current average pH potential?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //User clicked ok, reset calibration voltage
                                    if(averageCalVoltageValid) {
                                        phCalV10 = averageCalVoltage_pH;
                                        sharedPrefs.edit().putString(Prefs.PREF_PH10_VAL,String.valueOf(phCalV10)).apply();
                                        phCalSlopeHi = (phCalOffset - phCalV10) / (-3);
                                        sharedPrefs.edit().putString("pref_cal_phslopehi",String.valueOf(phCalSlopeHi)).apply();
                                    }
                                    updateSettingsDisplay();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            }
        });


        //Temp 0 Calibrate button and click listener
        btnCal_tLo = findViewById(R.id.btnCal_Tlow);
        btnCal_tLo.setVisibility(View.INVISIBLE);
        btnCal_tLo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (averageCalVoltageValid) {
                    new AlertDialog.Builder(MeasurementActivity.this)
                            .setTitle("Update Calibration Voltage")
                            .setMessage("Do you really want to update the calibration voltage for T = 0&#176;C with the current average temperature potential?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //User clicked ok, reset calibration voltage
                                    if(averageCalVoltageValid) {
                                        tCalOffset = averageCalVoltage_t;
                                        sharedPrefs.edit().putString("pref_cal_toffset",String.valueOf(tCalOffset)).apply();
                                        tCalSlope = (tCalV100 - tCalOffset) / 100;
                                        sharedPrefs.edit().putString("pref_cal_tslope",String.valueOf(tCalSlope)).apply();
                                    }
                                    updateSettingsDisplay();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            }
        });

        //Temp 100 Calibrate button and click listener
        btnCal_tHi = findViewById(R.id.btnCal_Thigh);
        btnCal_tHi.setVisibility(View.INVISIBLE);
        btnCal_tHi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (averageCalVoltageValid) {
                    new AlertDialog.Builder(MeasurementActivity.this)
                            .setTitle("Update Calibration Voltage")
                            .setMessage("Do you really want to update the calibration voltage for T = 100&#176;C with the current average temperature potential?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //User clicked ok, reset calibration voltage
                                    if(averageCalVoltageValid) {
                                        tCalV100 = averageCalVoltage_t;
                                        sharedPrefs.edit().putString(Prefs.PREF_T100_VAL,String.valueOf(tCalV100)).apply();
                                        tCalSlope = (tCalV100 - tCalOffset) / 100;
                                        sharedPrefs.edit().putString("pref_cal_tslope",String.valueOf(tCalSlope)).apply();
                                    }
                                    updateSettingsDisplay();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getValsFromPrefs();

        setCalView();
        updateSettingsDisplay();
        updateDisplayCalButton();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_NOTIFY_IF_UPDATED:
                Log.i(TAG, "Settings launched request update notification");
                if (resultCode == REQUEST_CODE_NOTIFY_IF_UPDATED) {
                    // User updated a preference, clear current data
                    clearData();
                }
                break;
            case REQUEST_CODE_SAVE_TO_DRIVE:
                if (resultCode == DriveExport.RESULT_CODE_SAVE_FAILED) {
                    showMessage(data.getStringExtra(DriveExport.EXTRAS_FAIL_DESCRIPTION));
                } else if (resultCode == DriveExport.RESULT_CODE_SAVE_SUCCESS) {
                    showMessage("Measured data successfully saved to Drive!");
                }
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startMeasuring(false);
        unbindService(serviceConnection);
        bluetoothLeService = null;
    }

    @Override
    public void onConnected(Bundle bundle) {

        Location l = getLastLoc();
        if(l!=null){
            double t = ((double)(System.currentTimeMillis() - l.getTime()))/1000.0;
            Log.d(TAG, "onConnected: Location received:" + l.toString() + " , " + String.format(Locale.CANADA,"%.2f",t) + " seconds ago");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed");
    }

    private Location getLastLoc(){
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLastLoc: Location permissions not granted!");
            return null;
        }
        if (!mGoogleApiClient.isConnected()){
            Log.d(TAG, "getLastLoc: Google API not connected!");
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.meas_menu, menu);
        switch (activeView){
            case VIEW_CAL:
                if(calpHViewActive){
                    menu.findItem(R.id.menu_change_view_cal_pH).setVisible(false);
                    menu.findItem(R.id.menu_change_view_cal_T).setVisible(true);
                } else {
                    menu.findItem(R.id.menu_change_view_cal_pH).setVisible(true);
                    menu.findItem(R.id.menu_change_view_cal_T).setVisible(false);
                }

                menu.findItem(R.id.menu_change_view_meas).setVisible(true);
                break;
            case VIEW_MEAS:
                menu.findItem(R.id.menu_change_view_cal_pH).setVisible(true);
                menu.findItem(R.id.menu_change_view_cal_T).setVisible(true);
                menu.findItem(R.id.menu_change_view_meas).setVisible(false);
                break;
            // VIEW_MEAS action for default case
            default:
                menu.findItem(R.id.menu_change_view_cal_pH).setVisible(true);
                menu.findItem(R.id.menu_change_view_cal_T).setVisible(true);
                menu.findItem(R.id.menu_change_view_meas).setVisible(false);
                activeView = VIEW_MEAS;
                break;
        }
        if (!measuring) {
            menu.findItem(R.id.menu_start).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
        } else {
            menu.findItem(R.id.menu_start).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_start:
                startMeasuring(true);
                invalidateOptionsMenu();
                //showMessage("Start recording");
                return true;
            case R.id.menu_stop:
                startMeasuring(false);
                invalidateOptionsMenu();
                //showMessage("Stop recording");
                return true;
            case R.id.menu_clear:
                startMeasuring(false);
                clearData();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_export_local:
                startMeasuring(false);
                //showMessage("Exporting to phone...");
                invalidateOptionsMenu();
                LocalExport localExport = new LocalExport(formFileName(),formDataArray(true));
                if(localExport.getWriteCompleteStatus() == LocalExport.STATUS_COMPLETE){
                    showMessage("Measured data successfully saved to file: " + localExport.getFileNameAndPath());
                } else {
                    showMessage("Error: Failed to save file!");
                }

                return true;
            case R.id.menu_export_cloud:
                startMeasuring(false);
                //showMessage("Exporting to Drive...");
                invalidateOptionsMenu();
                Intent intentDrive = new Intent(this, DriveExport.class);
                intentDrive.putStringArrayListExtra(DriveExport.EXTRAS_DATA_ARRAY,formDataArray(true));
                intentDrive.putExtra(DriveExport.EXTRAS_FILENAME,formFileName());
                startActivityForResult(intentDrive, REQUEST_CODE_SAVE_TO_DRIVE);

                return true;
            case R.id.menu_change_view_cal_pH:
                calpHViewActive = true;
                setView(VIEW_CAL);
                invalidateOptionsMenu();
                return true;
            case R.id.menu_change_view_meas:
                setView(VIEW_MEAS);
                invalidateOptionsMenu();
                return true;
            case R.id.menu_change_view_cal_T:
                calpHViewActive = false;
                setView(VIEW_CAL);
                invalidateOptionsMenu();
                return true;
            case R.id.menu_settings:
                Intent intentSettings = new Intent(this, Prefs.class);
                //start for result to notify if any preferences were updated
                startActivityForResult(intentSettings, REQUEST_CODE_NOTIFY_IF_UPDATED);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getValsFromPrefs(){
        double defVal;
        //Get preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        maxSampleSize = Integer.parseInt(sharedPrefs.getString("pref_samples",Prefs.DEF_SAMPLES));
        avgSampleSize = Integer.parseInt(sharedPrefs.getString("pref_average",Prefs.DEF_AVERAGE));
        displayAdvCal = sharedPrefs.getBoolean("pref_displayAdvCal", false);

        phCalOffset = Double.parseDouble(sharedPrefs.getString("pref_cal_ph7",Prefs.DEF_PHCALOFFSET));
        defVal = Double.valueOf(Prefs.DEF_PHCALOFFSET) + Double.valueOf(Prefs.DEF_PHCALSLOPE) * 3; //default ph10 potential
        phCalV10 = Double.parseDouble(sharedPrefs.getString(Prefs.PREF_PH10_VAL,String.valueOf(defVal)));
        defVal = Double.valueOf(Prefs.DEF_PHCALOFFSET) + Double.valueOf(Prefs.DEF_PHCALSLOPE) * -3; //default ph4 potential
        phCalV4 = Double.parseDouble(sharedPrefs.getString(Prefs.PREF_PH4_VAL,String.valueOf(defVal)));
        phCalSlopeLo = Double.parseDouble(sharedPrefs.getString("pref_cal_phslopelo",Prefs.DEF_PHCALSLOPE));
        phCalSlopeHi = Double.parseDouble(sharedPrefs.getString("pref_cal_phslopehi",Prefs.DEF_PHCALSLOPE));
        tCalOffset = Double.parseDouble(sharedPrefs.getString("pref_cal_toffset",Prefs.DEF_TCALOFFSET));
        defVal = Double.valueOf(Prefs.DEF_TCALOFFSET) + Double.valueOf(Prefs.DEF_TCALSLOPE) * 100; //default 100 deg potential
        tCalV100 = Double.parseDouble(sharedPrefs.getString(Prefs.PREF_T100_VAL,String.valueOf(defVal)));
        tCalSlope = Double.parseDouble(sharedPrefs.getString("pref_cal_tslope",Prefs.DEF_TCALSLOPE));

        ClCalOffset = Double.parseDouble(sharedPrefs.getString("pref_cal_cl_offset",Prefs.DEF_CLCALOFFSET));
        ClCalSlope = Double.parseDouble(sharedPrefs.getString("pref_cal_cl_slope",Prefs.DEF_CLCALSLOPE));
        ClCalLevel = Double.parseDouble(sharedPrefs.getString("pref_cal_cl_level",Prefs.DEF_CLCALLEVEL));

        alkCalOffset = Double.parseDouble(sharedPrefs.getString("pref_cal_alk_offset",Prefs.DEF_ALKCALOFFSET));
        alkCalSlope = Double.parseDouble(sharedPrefs.getString("pref_cal_alk_slope",Prefs.DEF_ALKCALSLOPE));
        alkCalLevel = Double.parseDouble(sharedPrefs.getString("pref_cal_alk_level",Prefs.DEF_ALKCALLEVEL));


    }

    private void setView(int viewNum){

        switch (viewNum){
            case VIEW_CAL:
                viewCal.setVisibility(View.VISIBLE);
                viewMeas.setVisibility(View.GONE);
                activeView = VIEW_CAL;
                setCalView();

                break;
            case VIEW_MEAS:
                viewCal.setVisibility(View.GONE);
                viewMeas.setVisibility(View.VISIBLE);
                activeView = VIEW_MEAS;
                break;
            // VIEW_MEAS action for default case
            default:
                viewCal.setVisibility(View.GONE);
                viewMeas.setVisibility(View.VISIBLE);
                activeView = VIEW_MEAS;
                break;
        }

    }
    private void setCalView(){
        if(displayAdvCal){
            //In adv view
            viewCalAdvDisplay.setVisibility(View.VISIBLE);
            viewCalStdDisplay.setVisibility(View.GONE);
        } else {
            //In Std. view
            viewCalStdDisplay.setVisibility(View.VISIBLE);
            viewCalAdvDisplay.setVisibility(View.GONE);
        }
        int phVis, tVis;
        phVis = (calpHViewActive) ? View.VISIBLE : View.GONE;
        tVis = (!calpHViewActive) ? View.VISIBLE : View.GONE;
        tvCalpHLbl[0].setVisibility(phVis);
        tvCalpHLbl[1].setVisibility(phVis);
        tvCalpHLbl[2].setVisibility(phVis);

        tvAvgVals[4].setVisibility(phVis); //avg ph raw adv
        tvAvgVals[5].setVisibility(phVis); //avg ph raw

        tvCalTLbl[0].setVisibility(tVis);
        tvCalTLbl[1].setVisibility(tVis);
        tvCalTLbl[2].setVisibility(tVis);

        tvAvgVals[6].setVisibility(tVis); //avg t raw adv
        tvAvgVals[7].setVisibility(tVis); //avg t raw

        viewButtons_pH.setVisibility(phVis);
        viewButtons_T.setVisibility(tVis);
    }

    private void startMeasuring(boolean enable){
        if (enable){
            try {
                registerReceiver(mGattUpdateReceiver,makeGattUpdateIntentFilter());
            } catch (IllegalArgumentException e){
                Log.d(TAG, "startMeasuring: Register Receiver: " + e.toString());
            }
            measuring = true;
        } else {
            try {
                unregisterReceiver(mGattUpdateReceiver);
            } catch (IllegalArgumentException e){
                Log.d(TAG, "startMeasuring: Unregister Receiver: " + e.toString());
            }
            measuring = false;
        }
        ValidClRecorded = false;
        ValidAlkRecorded = false;
        setActionBarSubtitle();
    }

    private void setActionBarSubtitle(){
        if(getSupportActionBar()!=null){
            if(demoMode){
                getSupportActionBar().setSubtitle(getResources().getString(R.string.connected_demo));
                return;
            }

            if(BLE_Connected){
                if(measuring)
                    getSupportActionBar().setSubtitle(getResources().getString(R.string.connected_measuring));
                else
                    getSupportActionBar().setSubtitle(getResources().getString(R.string.connected_stopped));
            } else {
                getSupportActionBar().setSubtitle(getResources().getString(R.string.disconnected));
            }

        } else {
            Log.e(TAG, "setActionBarSubtitle: Should not be null!");
            finish();
        }
    }

    private void setDemoMode(boolean en){
        if(en){
            demoMode = true;
            setActionBarSubtitle();
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
        } else {
            demoMode = false;
            setActionBarSubtitle();
            timerHandler.removeCallbacks(timerRunnable);

        }


    }
    // BLE DEVICE CONTROL
    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            if (!bluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.d(TAG, "Service Initialized");
            // Automatically connects to the device upon successful start-up initialization.
            if(!deviceName.equals("DEMOMODE")) {
                setDemoMode(false);
                bluetoothLeService.connect(deviceAddress);
            }
            else {
                setDemoMode(true);

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState(true);

                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState(false);
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.

                writeCharacteristic = null;
                enableWrite = false;

                detectGattServices(bluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                String s = new String(data);
                Log.d(TAG, "onReceive: " + s);

                // TODO TEMP



                //add data to form string, if NL encountered parse and update data
                //TODO I believe i parse the data here and include the alkalinity sensor stuff
                for (byte b:
                        data) {
                    if((char)b == '\n'){
                        if (sbRead.length() <= MAX_READ_LINE_LEN){
                            double[] d = new double[6];
                            if(parseDataSwCl(sbRead,d)) {
                                readFails = 0;
                                if (measuring)
                                    d[3] = d[2];
                                updateDataSwClAlk(d[0],d[1],d[2],d[3], d[4], d[5]>0.5);
                            } else {
                                //parse failed, if three consecutive fails prompt user
                                readFails++;
                                if (readFails > 2)
                                    showMessage("Error: could not parse received data!");
                            }
                        }
                        sbRead.setLength(0);
                    } else {
                        sbRead.append((char) b);
                    }
                }
            }
        }
    };


    void showMessage(String s){
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    boolean getConnected(){
        return BLE_Connected;
    }

    // Iterates through the supported GATT Services/Characteristics.
    private void detectGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        gattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristicList =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristicList) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, GattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);

                //Check for matching characteristic
                final int charaProp = gattCharacteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    Log.d(TAG,uuid + " has read characteristic");

                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    Log.d(TAG,uuid + " has notify characteristic");
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    Log.d(TAG,uuid + " has write characteristic");
                }

                if(currentCharaData.containsValue(GattAttributes.BLE_SHIELD_CHANNEL)){

                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (notifyCharacteristic != null) {
                        bluetoothLeService.setCharacteristicNotification(
                                notifyCharacteristic, false);
                        notifyCharacteristic = null;
                    }
                    // set read characteristic to BLE Shield
                    bluetoothLeService.readCharacteristic(gattCharacteristic);
                    // set notify characteristic to BLE Shield
                    notifyCharacteristic = gattCharacteristic;
                    bluetoothLeService.setCharacteristicNotification(
                            gattCharacteristic, true);
                    // set write characteristic to BLE Shield
                    writeCharacteristic = gattCharacteristic;
                    enableWrite = true;

                }
                gattCharacteristicGroupData.add(currentCharaData);

            }
            gattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateConnectionState(boolean connected) {

        BluetoothDevice bd = bluetoothLeService.getDevice();
        if (connected && bd != null) {
            //msgToast("Connected");
            BLE_Connected = true;

        } else {
            //msgToast("Disconnected");
            BLE_Connected = false;
        }
        setActionBarSubtitle();
    }

    private boolean parseData(StringBuilder sb, double[] data){
        StringBuilder[] dataStrings = new StringBuilder[3];
        for (int i = 0; i < dataStrings.length; i++) {
            dataStrings[i] = new StringBuilder();
        }

        if (data.length != 3){//invalid array length
            Log.e(TAG, "parseData: Invalid array size");
            return false;
        }
        int dataIndex = 0;
        boolean periodFound = false;

        /*iterate through supplied sb parameter, separate into StringBuilder array
        if data is valid (contains only numbers or one period per string element)
        and if max number of delimiters (2) is not exceeded.
        First and last read character must be a space*/
        int index = 0;
        boolean spaceExpected;
        for (char c:
             sb.toString().toCharArray()) {
            spaceExpected = index==0 || (index == sb.length()-1);
            if(spaceExpected && c != ' ') {
                //First char not space, invalid data
                Log.d(TAG, "parseData: Invalid string - space char ' ' expected");
                return false;
            } else if(!spaceExpected && c == ' '){ //data delimiter encountered
                dataIndex++;
                periodFound = false;
                if(dataIndex > 2){
                    Log.d(TAG, "parseData: Invalid string - too many space ' ' chars in parsed data");
                    return false;
                }
            } else if(!spaceExpected && isNum(c)){
                dataStrings[dataIndex].append(c);
            } else if(!spaceExpected && isPeriod(c)){
                if(periodFound){
                    Log.d(TAG, "parseData: Invalid string - too many period '.' chars in parsed data");
                    return false;
                }
                dataStrings[dataIndex].append(c);
                periodFound = true;
            }
            index++;
        }

        // data parsed into string array, convert and assign to double array
        for (int i = 0; i < 3; i++) {
            if (dataStrings[i].length()>0){
                try {
                    data[i] = Double.parseDouble(dataStrings[i].toString());
                } catch (Exception e){
                    Log.e(TAG, "parseData: Exception occurred: " +e.toString());
                }
            } else {
                Log.d(TAG, "parseData: Invalid string - empty data point found");
                return false;
            }
        }
        return true;
    }

    private boolean parseDataSwCl(StringBuilder sb, double[] data){
        Log.d(TAG, "parseData: Parsing string:"+sb.toString());
        int dataPts = 6; //expected number of data points per string
        StringBuilder[] dataStrings = new StringBuilder[dataPts];
        for (int i = 0; i < dataStrings.length; i++) {
            dataStrings[i] = new StringBuilder();
        }

        if (data.length != dataPts){//invalid array length
            Log.e(TAG, "parseData: Invalid array size");
            return false;
        }
        int dataIndex = 0;
        boolean periodFound = false;

        /*iterate through supplied sb parameter, separate into StringBuilder array
        if data is valid (contains only numbers or one period per string element)
        and if max number of delimiters (dataPts - 1) is not exceeded.
        First and last read character must be a space*/
        int index = 0;
        boolean spaceExpected;
        for (char c:
                sb.toString().toCharArray()) {
            spaceExpected = index==0 || (index == sb.length()-1);
             if(spaceExpected && c != ' ') {
                //First char not space, invalid data
                Log.d(TAG, "parseData: Invalid string - space char ' ' expected");
                return false;
            } else if(!spaceExpected && c == ' '){ //data delimiter encountered
                dataIndex++;
                periodFound = false;
                if(dataIndex > (dataPts-1)){
                    Log.d(TAG, "parseData: Invalid string - too many space ' ' chars in parsed data");
                    return false;
                }
            } else if(!spaceExpected && isNum(c)){
                dataStrings[dataIndex].append(c);
            } else if(!spaceExpected && isPeriod(c)){
                if(periodFound){
                    Log.d(TAG, "parseData: Invalid string - too many period '.' chars in parsed data");
                    return false;
                }
                dataStrings[dataIndex].append(c);
                periodFound = true;
            }
            index++;
        }

        // data parsed into string array, convert and assign to double array
        for (int i = 0; i < dataPts; i++) {
            if (dataStrings[i].length()>0){
                try {
                    //TODO hot-wiring the alkalinity sensor data
                    if (i == 3) {
                        data[3] = data[2];
                        data[4] = Double.parseDouble(dataStrings[3].toString());
                        data[5] = Double.parseDouble(dataStrings[4].toString());
                        break;
                    }
                    data[i] = Double.parseDouble(dataStrings[i].toString());
                } catch (Exception e){
                    Log.e(TAG, "parseData: Exception occurred: " +e.toString());
                }
            } else {
                Log.d(TAG, "parseData: Invalid string - empty data point found, i = " + i);
                return false;
            }
        }
        return true;
    }

    private boolean isNum(char c){
        return (c=='-'||c=='0'||c=='1'||c=='2'||c=='3'||c=='4'||c=='5'||c=='6'||c=='7'||c=='8'||c=='9');
    }
    private boolean isPeriod(char c){
        return (c=='.');
    }

    private boolean updateData(double t, double e, double i, double alk){
        boolean success;
        double avgValues[] = new double[5];
        double pH_stats[] = new double[5];
        double t_stats[] = new double[5];
        MeasData m = new MeasData(t,e,i,alk, phCalOffset,phCalSlopeLo,phCalSlopeHi,tCalOffset,tCalSlope);
        measList.add(m);

        if(measList.size() > maxSampleSize)
            measList.remove(0);
        // calculate average values, return if averaging operation was successful
        success = true;// calcAverages(avgValues, avgSampleSize);


        // If averages updated successfully, update average voltage and status
        // (Used for calibration)
        // Also calculate stats over average period
      //  if(success){
            averageCalVoltage_pH = avgValues[4];
            averageCalVoltage_t = avgValues[5];
            averageCalVoltageValid = true;
        //} else {
          //  averageCalVoltage_pH = 0.0;
           // averageCalVoltage_t = 0.0;
            //averageCalVoltageValid = false;
       // }

        // Hide or show Cal. buttons based on average calc. success
        updateDisplayCalButton();

        // update display with new values, update averages if average calc. successful
        success = refreshDisplayValues(averageCalVoltageValid,avgValues,m);

        //Update charts

        // update calibration chart
        List<Double> pH_valuesList = getDoubleFromMeasList(avgSampleSize, MeasData.RAW_VOLTAGE);
        List<Double> t_valuesList = getDoubleFromMeasList(avgSampleSize, MeasData.RAW_TEMPERATURE);
        if (averageCalVoltageValid) {
            //calculate stats
            pH_stats = calcLR.getStats(pH_valuesList);
            t_stats = calcLR.getStats(t_valuesList);
            //add stats to measList, retrieve last added element
            if(INCLUDE_STATS) {
                measList.get(measList.size() - 1).setpH_stats(pH_stats);
                measList.get(measList.size() - 1).setT_stats(t_stats);
            }
            if(calpHViewActive)
                refreshCalDisplayValues(Collections.max(pH_valuesList) - Collections.min(pH_valuesList),pH_stats);
            else
                refreshCalDisplayValues(Collections.max(t_valuesList) - Collections.min(t_valuesList),t_stats);
        } else {
            clearCalDisplayValues();
        }
        if(calpHViewActive)
            updateChartSeries(pH_valuesList,(SimpleXYSeries)calPlotSeries,calPlot);
        else
            updateChartSeries(t_valuesList,(SimpleXYSeries)calPlotSeries,calPlot);

        // update measurement charts, max sample size for meas plots = 1800
        // (30 mins @ 1Hz sampling),otherwise interface bogs down. Can be increased
        // if multi-threading is used or code is optimized
        updateChartSeries(getDoubleFromMeasList(1800, MeasData.CALC_TEMPERATURE),
                (SimpleXYSeries)measPlotSeries_T, measPlot_T);

        updateChartSeries(getDoubleFromMeasList(1800, MeasData.CALC_PH),
                (SimpleXYSeries)measPlotSeries_pH, measPlot_pH);

        updateChartSeries(getDoubleFromMeasList(1800, MeasData.CALC_CL),
                (SimpleXYSeries)measPlotSeries_Cl, measPlot_Cl);

        updateChartSeries(getDoubleFromMeasList(1800, MeasData.CALC_ALK),
                (SimpleXYSeries)measPlotSeries_alk, measPlot_alk);


        return success;
    }

    //Update data utlizing switched free Cl measurements
    // TODO add parameters to include the alkalinity sensor
    private boolean updateDataSwClAlk(double t, double e, double i, double a, double tMeas, boolean swOn){
        boolean success;
        double avgValues[] = new double[6];
        double pH_stats[] = new double[6];
        double t_stats[] = new double[6];
        MeasData m = new MeasData(t,e,i,a, tMeas,swOn,phCalOffset,phCalSlopeLo,phCalSlopeHi,tCalOffset,tCalSlope,ClCalOffset,ClCalLevel,ClCalSlope, alkCalOffset, alkCalLevel, alkCalSlope);

        measList.add(m);

        if(measList.size() > maxSampleSize)
            measList.remove(0);
        // calculate average values, return if averaging operation was successful
        success = calcAverages(avgValues, avgSampleSize);


        // If averages updated successfully, update average voltage and status
        // (Used for calibration)
        // Also calculate stats over average period
        if(success){
            averageCalVoltage_pH = avgValues[4];
            averageCalVoltage_t = avgValues[5];
            averageCalVoltageValid = true;
        } else {
            averageCalVoltage_pH = 0.0;
            averageCalVoltage_t = 0.0;
            averageCalVoltageValid = false;
        }

        // Hide or show Cal. buttons based on average calc. success
        updateDisplayCalButton();

        // update display with new values, update averages if average calc. successful
        success = refreshDisplayValues(averageCalVoltageValid,avgValues,m);


        //Update charts

        // update calibration chart
        List<Double> pH_valuesList = getDoubleFromMeasList(avgSampleSize, MeasData.RAW_VOLTAGE);
        List<Double> t_valuesList = getDoubleFromMeasList(avgSampleSize, MeasData.RAW_TEMPERATURE);
        if (averageCalVoltageValid) {
            //calculate stats
            pH_stats = calcLR.getStats(pH_valuesList);
            t_stats = calcLR.getStats(t_valuesList);
            //add stats to measList, retrieve last added element
            if(INCLUDE_STATS) {
                measList.get(measList.size() - 1).setpH_stats(pH_stats);
                measList.get(measList.size() - 1).setT_stats(t_stats);
            }
            if(calpHViewActive)
                refreshCalDisplayValues(Collections.max(pH_valuesList) - Collections.min(pH_valuesList),pH_stats);
            else
                refreshCalDisplayValues(Collections.max(t_valuesList) - Collections.min(t_valuesList),t_stats);
        } else {
            clearCalDisplayValues();
        }
        if(calpHViewActive)
            updateChartSeries(pH_valuesList,(SimpleXYSeries)calPlotSeries,calPlot);
        else
            updateChartSeries(t_valuesList,(SimpleXYSeries)calPlotSeries,calPlot);
        Log.d(TAG, "updateDataSwClAlk: See if this function is being called");
        // update measurement charts, max sample size for meas plots = 1800
        // (30 mins @ 1Hz sampling),otherwise interface bogs down. Can be increased
        // if multi-threading is used or code is optimized
        updateChartSeries(getDoubleFromMeasList(1800, MeasData.CALC_TEMPERATURE),
                (SimpleXYSeries)measPlotSeries_T, measPlot_T);

        updateChartSeries(getDoubleFromMeasList(1800, MeasData.CALC_PH),
                (SimpleXYSeries)measPlotSeries_pH, measPlot_pH);

        updateChartSeries(getDoubleFromMeasList(1800, MeasData.CALC_CL),
                (SimpleXYSeries)measPlotSeries_Cl, measPlot_Cl);

        updateChartSeries(getDoubleFromMeasList(1800, MeasData.CALC_ALK),
                (SimpleXYSeries)measPlotSeries_alk, measPlot_alk);

        return success;
    }

    private List<Number> getNumbersFromMeasList(int maxLength, int valIndex){
        //length of returned list is less of provided maxLength and measList size
        int l = (maxLength < measList.size()) ? maxLength : measList.size();

        List<Number> numbers = new ArrayList<>();
        for (int i = measList.size()-l; i < measList.size(); i++) {
            numbers.add((Number)measList.get(i).getValue(valIndex));
        }
        return numbers;
    }

    private List<Double> getDoubleFromMeasList(int maxLength, int valIndex){
        //length of returned list is less of provided maxLength and measList size
        int l = (maxLength < measList.size()) ? maxLength : measList.size();

        List<Double> values = new ArrayList<>();
        Log.d(TAG, "getDoubleFromMeasList: measLIst" + measList.toString());
        for (int i = measList.size()-l; i < measList.size(); i++) {
            Log.d(TAG, "getDoubleFromMeasList: item at index" + measList.get(i).getValue(valIndex) );
            values.add((Double)measList.get(i).getValue(valIndex));
        }
        return values;
    }

    private void updateChartSeries(Number[] seriesData, SimpleXYSeries series, XYPlot plot) {
        List<Number> numbers = new ArrayList<>(seriesData.length);
        Collections.addAll(numbers, seriesData);
        series.setModel(numbers, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
        plot.redraw();
    }

    private void updateChartSeries(List<? extends Number> numbers, SimpleXYSeries series, XYPlot plot) {
        series.setModel(numbers, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
        plot.redraw();
    }

    private boolean refreshDisplayValues(boolean updateAvg, double[] avg, MeasData m) {
        try {
            boolean CurrClValid = (double)m.getValue(MeasData.SW_TIME) > CL_MEAS_THRESHOLD;
            boolean CurrAlkValid = (double)m.getValue(MeasData.SW_TIME) > ALK_MEAS_THRESHOLD;

            if(CurrClValid){
                ValidClRecorded = true;
            }
            if(CurrAlkValid){
                ValidAlkRecorded = true;
            }

            //update current values
            //The 4 is for the different types of measurements: pH (0), T (1), Cl (2), Alk (3)
            for (int i = 0; i < 4; i++) {
                if(i != MeasData.CALC_CL && i != MeasData.CALC_ALK) {
                    tvCurrentVals[i].setText(String.format(Locale.CANADA, "%.2f", (double) m.getValue(i)));
                } else if (i == MeasData.CALC_CL){
                    if (CurrClValid) {
                        tvCurrentVals[i].setText(String.format(Locale.CANADA, "%.2f", (double) m.getValue(i)));
                        tvCurrentVals[i].setTextSize(24);
                        if((double)m.getValue(i) >= 0.1 && (double)m.getValue(i) <= 4.0){
                            tvCurrentVals[i].setTextColor(Color.GREEN);
                        } else {
                            tvCurrentVals[i].setTextColor(Color.BLACK);
                        }
                    } else if (!ValidClRecorded) {
                        tvCurrentVals[i].setText(getResources().getString(R.string.cl_not_valid));
                        tvCurrentVals[i].setTextSize(10);
                        tvCurrentVals[i].setTextColor(Color.BLACK);
                    }
                } else { // The condition for when i == MeasData_CALC_ALK
                    if (CurrAlkValid) {
                        tvCurrentVals[i].setText(String.format(Locale.CANADA, "%.2f", (double) m.getValue(i)));
                        tvCurrentVals[i].setTextSize(24);
                        // TODO get the proper ppm values
                        if((double)m.getValue(i) >= 75 && (double)m.getValue(i) <= 238){
                            tvCurrentVals[i].setTextColor(Color.GREEN);
                        } else {
                            tvCurrentVals[i].setTextColor(Color.BLACK);
                        }
                    } else if (!ValidAlkRecorded) {
                        tvCurrentVals[i].setText(getResources().getString(R.string.alk_not_valid));
                        tvCurrentVals[i].setTextSize(10);
                        tvCurrentVals[i].setTextColor(Color.BLACK);
                    }

                }
            }

            //format pH voltage with one decimal place
            tvCurrentVals[4].setText(String.format(Locale.CANADA,"%.1f", (double)m.getValue(4)));

            //format Cl current with one decimal place
            tvCurrentVals[5].setText(String.format(Locale.CANADA,"%.1f", (double)m.getValue(5)));

            if (updateAvg) {
                //update average values
                for (int i = 0; i < 4; i++) {
                    if(i != MeasData.CALC_CL && i != MeasData.CALC_ALK) {
                        tvAvgVals[i].setText(String.format(Locale.CANADA, "%.2f", avg[i]));
                    } else if (i == MeasData.CALC_CL) {
                        if (CurrClValid) {
                            tvAvgVals[i].setText(String.format(Locale.CANADA, "%.2f", (double) avg[i]));
                            tvAvgVals[i].setTextSize(24);
                            if(avg[i] >= 0.1 && avg[i] <= 4.0){
                                tvAvgVals[i].setTextColor(Color.GREEN);
                            } else {
                                tvAvgVals[i].setTextColor(Color.BLACK);
                            }
                        } else if (!ValidClRecorded) {
                            tvAvgVals[i].setText(getResources().getString(R.string.cl_not_valid));
                            tvAvgVals[i].setTextSize(10);
                            tvAvgVals[i].setTextColor(Color.BLACK);
                        }
                    } else if (i == MeasData.CALC_ALK) {
                        if (CurrAlkValid) {
                            tvAvgVals[i].setText(String.format(Locale.CANADA, "%.2f", (double) avg[i]));
                            tvAvgVals[i].setTextSize(24);
                            if(avg[i] >= 0.1 && avg[i] <= 4.0){
                                tvAvgVals[i].setTextColor(Color.GREEN);
                            } else {
                                tvAvgVals[i].setTextColor(Color.BLACK);
                            }
                        } else if (!ValidAlkRecorded) {
                            tvAvgVals[i].setText(getResources().getString(R.string.alk_not_valid));
                            tvAvgVals[i].setTextSize(10);
                            tvAvgVals[i].setTextColor(Color.BLACK);
                        }
                    }
                }
                //format pH voltage avg with one decimal place
                tvAvgVals[4].setText(String.format(Locale.CANADA,"%.1f", avg[4])); //ph V Avg, advanced calib. screen
                tvAvgVals[5].setText(String.format(Locale.CANADA,"%.1f", avg[4])); //ph V Avg, standard calib. screen
                //format temp voltage avg with one decimal place
                tvAvgVals[6].setText(String.format(Locale.CANADA,"%.1f", avg[5])); //t V Avg, advanced calib. screen
                tvAvgVals[7].setText(String.format(Locale.CANADA,"%.1f", avg[5])); //t V Avg, standard calib. screen

            } else {
                // clear average values
                for (int i = 0; i < 6; i++) {
                    tvAvgVals[i].setText(getResources().getString(R.string.no_val));
                }
            }
            //update number of samples display
            tvSamples.setText(String.format(Locale.CANADA,"%d", measList.size()));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "refreshDisplayValues: Exception occurred: " + e.toString());
            return false;
        }
    }

    private void refreshCalDisplayValues(double range, double[] stats){

        for (int i = 0; i < 4; i++) {
            if(!Double.isNaN(stats[i])){
                tvStats[i].setText(String.format(Locale.CANADA,"%.2f", stats[i])); //set tv if value is num
            }
            else
                tvStats[i].setText(getResources().getString(R.string.no_val)); //else clear (NaN)
        }
        // last value in array is score, format to one decimal and apply to two tv's
        if(!Double.isNaN(stats[4])){
            tvStats[4].setText(String.format(Locale.CANADA,"%.1f", stats[4]));
            tvStats[5].setText(String.format(Locale.CANADA,"%.1f", stats[4]));
            //set color based on range values falls in
            if(stats[4]<65){
                tvStats[4].setTextColor(Color.RED);
                tvStats[5].setTextColor(Color.RED);
            } else if(stats[4]>=65&&stats[4]<80){
                tvStats[4].setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                tvStats[5].setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else if(stats[4]>=80){
                tvStats[4].setTextColor(Color.GREEN);
                tvStats[5].setTextColor(Color.GREEN);
            }

        } else {
            tvStats[4].setText(getResources().getString(R.string.no_val));
            tvStats[5].setText(getResources().getString(R.string.no_val));
            tvStats[4].setTextColor(Color.BLACK);
            tvStats[5].setTextColor(Color.BLACK);
        }
        // set peak-peak value
        tvStats[6].setText(String.format(Locale.CANADA,"%.1f", range));


    }

    private void clearCalDisplayValues(){
        // clear cal display values
        for (int i = 0; i < 6; i++) {
            tvStats[i].setText(getResources().getString(R.string.no_val));
        }
        tvStats[4].setTextColor(Color.BLACK);
        tvStats[5].setTextColor(Color.BLACK);
    }

    private void updateSettingsDisplay(){
        tvEcal.setText(String.format(Locale.CANADA,"%.1f", phCalOffset));
        tvSampSize.setText(String.format(Locale.CANADA,"%d", maxSampleSize));
        tvAvgSize.setText(String.format(Locale.CANADA,"%d", avgSampleSize));
    }

    private void updateDisplayCalButton(){
        if(averageCalVoltageValid) {
            btnCal_pH4.setVisibility(View.VISIBLE);
            btnCal_pH7.setVisibility(View.VISIBLE);
            btnCal_pH10.setVisibility(View.VISIBLE);
            btnCal_tLo.setVisibility(View.VISIBLE);
            btnCal_tHi.setVisibility(View.VISIBLE);
        } else {
            btnCal_pH4.setVisibility(View.INVISIBLE);
            btnCal_pH7.setVisibility(View.INVISIBLE);
            btnCal_pH10.setVisibility(View.INVISIBLE);
            btnCal_tLo.setVisibility(View.INVISIBLE);
            btnCal_tHi.setVisibility(View.INVISIBLE);
        }
    }

    private void clearData(){
        measList.clear();
        clearDisplayValues();
    }

    private boolean clearDisplayValues(){
        try {
            //update current values
            for (int i = 0; i < 6; i++) {
                tvCurrentVals[i].setText(getResources().getString(R.string.no_val));
            }

            // clear average values
            for (int i = 0; i < 4; i++) {
                tvAvgVals[i].setText(getResources().getString(R.string.no_val));
            }
            clearCalDisplayValues();
            tvSamples.setText(getResources().getString(R.string.no_val));
            return true;
        } catch (Exception e){
            Log.e(TAG, "clearDisplayValues: Exception occurred: " +e.toString());
            return false;
        }
    }

    private boolean calcAverages(double[] avg, int samples){
        double scratch[] = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; // scratch double array for calculating sum
        if (avg.length != 6){//invalid array length
            Log.e(TAG, "calcAverages: Invalid array size");
            return false;
        }

        int s = measList.size();
        if(s >= samples){
            try{

                    for (int i = 0; i < samples; i++) {
                    scratch[0] = scratch[0] + (double)measList.get(s-i-1).getValue(MeasData.CALC_TEMPERATURE);
                    scratch[1] = scratch[1] + (double)measList.get(s-i-1).getValue(MeasData.CALC_PH);
                    scratch[2] = scratch[2] + (double)measList.get(s-i-1).getValue(MeasData.CALC_CL);
                    scratch[3] = scratch[3] + (double)measList.get(s-i-1).getValue(MeasData.CALC_ALK);
                    scratch[4] = scratch[4] + (double)measList.get(s-i-1).getValue(MeasData.RAW_TEMPERATURE);
                    scratch[5] = scratch[5] + (double)measList.get(s-i-1).getValue(MeasData.RAW_VOLTAGE);
                        //TODO add alkalinity here

                    }
                avg[0] = scratch[0]/samples;
                avg[1] = scratch[1]/samples;
                avg[2] = scratch[2]/samples;
                avg[3] = scratch[3]/samples;
                avg[4] = scratch[4]/samples;
                avg[5] = scratch[5]/samples;
            } catch (Exception e){
                Log.e(TAG, "calcAverages: Exception occurred: " +e.toString());
                return false;
            }
            return true;
        } else {
            return false;
        }

    }

    private ArrayList<String> formDataArray(boolean includeLocation){
        ArrayList<String> dataArray = new ArrayList<>();
        //Clear any data in array list
        dataArray.clear();

        //File info headers
        String[] headers = new String[6];
        //Column headers
        String[] ch = new String[]{
                "Time",
                "Temp.(C)",
                "pH Level",
                "Free Cl (ppm)",
                "Alkalinity (ppm)",
                "pH (mV)",
                "Free Cl (nA)",
                "Interval Time",
                "Free Cl Sw. On"
        };

        //Additional stats column headers
        String[] sch = new String[]{
                "Avg. Ok",
                "LR Slope",
                "LR Intcpt.",
                "LR R^2",
                "Std. Dev.",
                "Score"
        };

        //File header required info:
        String timeStamp = DateFormat.getDateTimeInstance().format(new Date());

        //Location Data
        String sLoc, sUrl;
        if(includeLocation){
            Location l = getLastLoc();
            if(l!=null){
                //Location found
                double t = ((double)(System.currentTimeMillis() - l.getTime()))/1000.0;
                Log.d(TAG, "exportData: Location received: " + l.toString() + " , " + String.format(Locale.CANADA,"%.3f",t) + " seconds ago");
                sLoc = String.format(Locale.CANADA,"%.6f,%.6f",l.getLatitude(),l.getLongitude());
                sUrl = "https://www.google.com/maps/search/?api=1&query="+sLoc;
            } else {
                //Location feature enabled, no location found or error encountered
                sLoc = "Location Not Found";
                sUrl = "Location Not Found";
            }
        } else {
            //Location feature disabled
            sLoc = "Feature disabled";
            sUrl = "Feature disabled";
        }

        //Assemble file header strings:

        //Top of file header/title
        headers[0] = TITLE_HEADER;
        //Date and time
        headers[1] = timeStamp;
        //Location
        headers[2] = "Recorded Location (Lat, Long): \t"+sLoc;
        //Location, google maps URL
        headers[3] = "Location Url: \t"+sUrl;
        //Calibration voltage
        headers[4] = "Calibration Voltage, Ecal [pH = 7] (mV): \t"+String.format(Locale.CANADA,"%.3f", phCalOffset);
        //Data column headers
        if(INCLUDE_STATS)
            headers[5] = ch[0]+'\t'+ch[1]+'\t'+ch[2]+'\t'+ch[3]+'\t'+ch[4]+'\t'+ch[5]+'\t'+ch[6]+'\t'+ch[7]+
                    sch[0]+'\t'+sch[1]+'\t'+sch[2]+'\t'+sch[3]+'\t'+sch[4]+'\t'+sch[5];
        else
            headers[5] = ch[0]+'\t'+ch[1]+'\t'+ch[2]+'\t'+ch[3]+'\t'+ch[4]+'\t'+ch[5]+'\t'+ch[6]+'\t'+ch[7];

        //Write String data to dataArray
        for (String s:
                headers) {
            dataArray.add(s);
        }
        //Write Data
        for (MeasData md:
                measList) {
            if(INCLUDE_STATS){
                double pH_stats[] = md.getpH_stats();
                double t_stats[] = md.getT_stats();
                dataArray.add(String.format(Locale.CANADA,"%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f" +
                                "\t%.2f\t%b" +
                                "\t%b\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
                        md.getValue(MeasData.TIME_STAMP),
                        md.getValue(MeasData.CALC_TEMPERATURE),
                        md.getValue(MeasData.CALC_PH),
                        md.getValue(MeasData.CALC_CL),
                        md.getValue(MeasData.CALC_ALK),
                        md.getValue(MeasData.RAW_VOLTAGE),
                        md.getValue(MeasData.RAW_CURRENT),
                        md.getValue(MeasData.RAW_ALK),
                        //free Cl sw meas info
                        md.getValue(MeasData.SW_TIME),
                        md.getValue(MeasData.CL_SW),
                        //measurement stats
                        md.getAvgOk(),
                        pH_stats[0],pH_stats[1],pH_stats[2],pH_stats[3],pH_stats[4],
                        t_stats[0],t_stats[1],t_stats[2],t_stats[3],t_stats[4]
                ));
            } else {
                dataArray.add(String.format(Locale.CANADA,"%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f"+"\t%.2f\t%b",
                        md.getValue(MeasData.TIME_STAMP),
                        md.getValue(MeasData.CALC_TEMPERATURE),
                        md.getValue(MeasData.CALC_PH),
                        md.getValue(MeasData.CALC_CL),
                        md.getValue(MeasData.CALC_ALK),
                        md.getValue(MeasData.RAW_VOLTAGE),
                        md.getValue(MeasData.RAW_CURRENT),
                        md.getValue(MeasData.RAW_ALK),
                        //free Cl sw meas info
                        md.getValue(MeasData.SW_TIME),
                        md.getValue(MeasData.CL_SW)
                ));
            }

        }
        return dataArray;
    }

    private String formFileName(){
        String filename = "wqm_results_" + new SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.CANADA).format(new Date()) + ".tsv";
        Log.i(TAG, "formFileName: filename; " + filename);
        return filename;
    }

}
