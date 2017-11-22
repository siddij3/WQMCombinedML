package ca.mcmaster.waterqualitymonitor;

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

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.*;

public class MeasurementActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MeasurementActivity.class.getSimpleName();

    //Extras Definitions
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static final int VIEW_MEAS = 0;
    private static final int VIEW_CAL = 1;

    public static final int MAX_READ_LINE_LEN = 32; //Maximum expected size in chars per msg
    private static final String TITLE_HEADER = "Water Quality Monitor Results"; //Exported file header title

    //Request Codes
    private static final int REQUEST_CODE_SAVE_TO_DRIVE = 1;
    public final static int REQUEST_CODE_NOTIFY_IF_UPDATED = 2; //Request code for starting Prefs activity

    private int activeView;
    private SharedPreferences sharedPrefs;

    private String deviceName;
    private String deviceAddress;

    private boolean measuring;
    private int readFails;

    private BluetoothLeService bluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> gattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean BLE_Connected = false;
    private boolean enableWrite = false;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private BluetoothGattCharacteristic writeCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    //GUI elements
    private Button btnCal;
    private View viewMeas;
    private View viewCal;

    private TextView[] tvCurrentVals = new TextView[6];
    private TextView[] tvAvgVals = new TextView[4];

    private StringBuilder sbRead = new StringBuilder();

    private TextView tvSamples;
    private TextView tvCalPhPkPk;

    //Plot Variables
    private XYPlot calPlot;
    private XYSeries calPlotSeries_pH;

    private XYPlot measPlot;
    private XYSeries measPlotSeries_pH;

    //Array list for storing measurements
    private ArrayList<MeasData> measList = new ArrayList<>();


    //Location Services
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    //Stored mean voltage and status for calibration
    double averageCalVoltage;
    boolean averageCalVoltageValid;

    //Preference/Settings display
    private TextView tvSampSize;
    private TextView tvAvgSize;
    private TextView tvEcal;

    //Values from preferences
    private int maxSampleSize;
    private int avgSampleSize;
    private double eCal;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Intent and extra data
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        tvCurrentVals[0] = (TextView) findViewById(R.id.val_currentTemp);
        tvCurrentVals[1] = (TextView) findViewById(R.id.val_currentPh);
        tvCurrentVals[2] = (TextView) findViewById(R.id.val_currentCl);
        tvCurrentVals[3] = (TextView) findViewById(R.id.val_TempRaw);
        tvCurrentVals[4] = (TextView) findViewById(R.id.val_PhRaw);
        tvCurrentVals[5] = (TextView) findViewById(R.id.val_ClRaw);

        tvAvgVals[0] = (TextView) findViewById(R.id.val_avgTemp);
        tvAvgVals[1] = (TextView) findViewById(R.id.val_avgPh);
        tvAvgVals[2] = (TextView) findViewById(R.id.val_avgCl);
        tvAvgVals[3] = (TextView) findViewById(R.id.val_avgPhRaw);

        tvSamples = (TextView) findViewById(R.id.val_Samples);

        tvSampSize  = (TextView) findViewById(R.id.val_samp_size);
        tvAvgSize  = (TextView) findViewById(R.id.val_avg_size);
        tvEcal  = (TextView) findViewById(R.id.val_ecal);

        tvCalPhPkPk  = (TextView) findViewById(R.id.val_PkPk);

        //Get preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        maxSampleSize = Integer.parseInt(sharedPrefs.getString("pref_samples",Prefs.DEF_SAMPLES));
        avgSampleSize = Integer.parseInt(sharedPrefs.getString("pref_average",Prefs.DEF_AVERAGE));
        eCal = Double.parseDouble(sharedPrefs.getString("pref_cal_ph7",Prefs.DEF_ECAL));

        //Update Settings display with values from preferences
        updateSettingsDisplay();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        viewCal = findViewById(R.id.layoutCal);
        viewMeas = findViewById(R.id.layoutMeas);
        setView(VIEW_MEAS);
        startMeasuring(true);
        readFails = 0;

        //Location Services using google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //XY Plots
        calPlot = (XYPlot) findViewById(R.id.calPlot);
        calPlotSeries_pH = new SimpleXYSeries("pH voltage (mV)");
        calPlot.addSeries(calPlotSeries_pH, new LineAndPointFormatter(Color.BLACK, null, null, null));

        measPlot = (XYPlot) findViewById(R.id.measPlot);
        measPlotSeries_pH = new SimpleXYSeries("pH Level");
        measPlot.addSeries(measPlotSeries_pH, new LineAndPointFormatter(Color.BLACK, null, null, null));

        //Calibrate button and click listener
        btnCal = findViewById(R.id.btnCal_pH7);
        btnCal.setVisibility(View.INVISIBLE);
        btnCal.setOnClickListener(new View.OnClickListener() {
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
                                        eCal = averageCalVoltage;
                                        sharedPrefs.edit().putString("pref_cal_ph7",String.valueOf(eCal)).commit();
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

        //Get preferences
        maxSampleSize = Integer.parseInt(sharedPrefs.getString("pref_samples",Prefs.DEF_SAMPLES));
        avgSampleSize = Integer.parseInt(sharedPrefs.getString("pref_average",Prefs.DEF_AVERAGE));
        eCal = Double.parseDouble(sharedPrefs.getString("pref_cal_ph7",Prefs.DEF_ECAL));

        updateSettingsDisplay();
        updateDisplayCal();

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
                menu.findItem(R.id.menu_change_view_cal).setVisible(false);
                menu.findItem(R.id.menu_change_view_meas).setVisible(true);
                break;
            case VIEW_MEAS:
                menu.findItem(R.id.menu_change_view_cal).setVisible(true);
                menu.findItem(R.id.menu_change_view_meas).setVisible(false);
                break;
            // VIEW_MEAS action for default case
            default:
                menu.findItem(R.id.menu_change_view_cal).setVisible(true);
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
                showMessage("Start recording");
                return true;
            case R.id.menu_stop:
                startMeasuring(false);
                invalidateOptionsMenu();
                showMessage("Stop recording");
                return true;
            case R.id.menu_clear:
                startMeasuring(false);
                clearData();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_export_local:
                startMeasuring(false);
                showMessage("Exporting to phone...");
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
                showMessage("Exporting to Drive...");
                invalidateOptionsMenu();
                Intent intentDrive = new Intent(this, DriveExport.class);
                intentDrive.putStringArrayListExtra(DriveExport.EXTRAS_DATA_ARRAY,formDataArray(true));
                intentDrive.putExtra(DriveExport.EXTRAS_FILENAME,formFileName());
                startActivityForResult(intentDrive, REQUEST_CODE_SAVE_TO_DRIVE);

                return true;
            case R.id.menu_change_view_cal:
                setView(VIEW_CAL);
                invalidateOptionsMenu();
                showMessage("Calibration View");
                return true;
            case R.id.menu_change_view_meas:
                setView(VIEW_MEAS);
                invalidateOptionsMenu();
                showMessage("Measurement View");
                return true;
            case R.id.menu_settings:
                Intent intentSettings = new Intent(this, Prefs.class);
                //start for result to notify if any preferences were updated
                startActivityForResult(intentSettings, REQUEST_CODE_NOTIFY_IF_UPDATED);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setView(int viewNum){

        switch (viewNum){
            case VIEW_CAL:
                viewCal.setVisibility(View.VISIBLE);
                viewMeas.setVisibility(View.GONE);
                activeView = VIEW_CAL;
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
            if(!deviceName.equals("TEST")) //todo temporary debug
                bluetoothLeService.connect(deviceAddress);
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


                //add data to form string, if NL encountered parse and update data
                for (byte b:
                        data) {
                    if((char)b == '\n'){
                        if (sbRead.length() <= MAX_READ_LINE_LEN){
                            double[] d = new double[3];
                            if(parseData(sbRead,d)) {
                                readFails = 0;
                                if (measuring)
                                    updateData(d[0],d[1],d[2],eCal);
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
            getSupportActionBar().setSubtitle("Connected: " + bd.getAddress());
            BLE_Connected = true;

        } else {
            //msgToast("Disconnected");
            getSupportActionBar().setSubtitle(getResources().getString(R.string.disconnected));
            BLE_Connected = false;
        }

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
    private boolean isNum(char c){ //todo update for proper negative sign detection ie remove from this fcn and only allow if it is first char
        return (c=='-'||c=='0'||c=='1'||c=='2'||c=='3'||c=='4'||c=='5'||c=='6'||c=='7'||c=='8'||c=='9');
    }
    private boolean isPeriod(char c){
        return (c=='.');
    }

    private boolean updateData(double t, double e, double i, double eCal){
        boolean success;
        double avgValues[] = new double[4];
        MeasData m = new MeasData(t,e,i,eCal);
        measList.add(m);

        if(measList.size() > maxSampleSize)
            measList.remove(0);
        // calculate average values, return if averaging operation was successful
        success = calcAverages(avgValues, avgSampleSize);


        // If averages updated successfully, update average voltage and status
        // (Used for calibration)
        if(success){
            averageCalVoltage = avgValues[3];
            averageCalVoltageValid = true;
        } else {
            averageCalVoltage = 0.0;
            averageCalVoltageValid = false;
        }
        // Hide or show Cal. button based on average calc. success
        updateDisplayCal();
        // update display with new values, update averages if average calc. successful
        success = refreshDisplayValues(success,avgValues,m);

        //Update charts
        if (activeView==VIEW_CAL){
            // update calibration chart
            List<Double> valuesList = getDoubleFromMeasList(avgSampleSize, MeasData.RAW_VOLTAGE);
            if (valuesList.size()>1) {
                //refresh pk-pk ph mV value if enough data exists to calculate
                refreshCalDisplayValues(Collections.max(valuesList) - Collections.min(valuesList));
            }
            updateChartSeries(valuesList,(SimpleXYSeries)calPlotSeries_pH,calPlot);
        } else if (activeView==VIEW_MEAS){
            // update measurement charts
            List<Double> valuesList = getDoubleFromMeasList(maxSampleSize, MeasData.CALC_PH);
            updateChartSeries(valuesList,(SimpleXYSeries)measPlotSeries_pH,measPlot);
        }

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
        for (int i = measList.size()-l; i < measList.size(); i++) {
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
            //update current values
            for (int i = 0; i < 4; i++) {
                tvCurrentVals[i].setText(String.format(Locale.CANADA,"%.2f", (double)m.getValue(i)));
            }

            //format pH voltage with one decimal place
            tvCurrentVals[4].setText(String.format(Locale.CANADA,"%.1f", (double)m.getValue(4)));

            //format Cl current with one decimal place
            tvCurrentVals[5].setText(String.format(Locale.CANADA,"%.1f", (double)m.getValue(5)));

            if (updateAvg) {
                //update average values
                for (int i = 0; i < 3; i++) {
                    tvAvgVals[i].setText(String.format(Locale.CANADA,"%.2f", avg[i]));
                }
                //format pH voltage avg with one decimal place
                tvAvgVals[3].setText(String.format(Locale.CANADA,"%.1f", avg[3]));
            } else {
                // clear average values
                for (int i = 0; i < 4; i++) {
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

    private void refreshCalDisplayValues(double range){
        tvCalPhPkPk.setText(String.format(Locale.CANADA,"%.1f", range));
    }

    private void updateSettingsDisplay(){
        tvEcal.setText(String.format(Locale.CANADA,"%.1f", eCal));
        tvSampSize.setText(String.format(Locale.CANADA,"%d", maxSampleSize));
        tvAvgSize.setText(String.format(Locale.CANADA,"%d", avgSampleSize));
    }

    private void updateDisplayCal(){
        if(averageCalVoltageValid) {
            btnCal.setVisibility(View.VISIBLE);
        } else {
            btnCal.setVisibility(View.INVISIBLE);
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
            tvCalPhPkPk.setText(getResources().getString(R.string.no_val));
            tvSamples.setText(getResources().getString(R.string.no_val));
            return true;
        } catch (Exception e){
            Log.e(TAG, "clearDisplayValues: Exception occurred: " +e.toString());
            return false;
        }
    }

    private boolean calcAverages(double[] avg, int samples){
        double scratch[] = new double[] {0.0,0.0,0.0,0.0}; // scratch double array for calculating sum
        if (avg.length != 4){//invalid array length
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
                    scratch[3] = scratch[3] + (double)measList.get(s-i-1).getValue(MeasData.RAW_VOLTAGE);
                }
                avg[0] = scratch[0]/samples;
                avg[1] = scratch[1]/samples;
                avg[2] = scratch[2]/samples;
                avg[3] = scratch[3]/samples;
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
                "pH (mV)",
                "Free Cl (nA)"
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
        headers[4] = "Calibration Voltage, Ecal [pH = 7] (mV): \t"+String.format(Locale.CANADA,"%.3f",eCal);
        //Data column headers
        headers[5] = ch[0]+'\t'+ch[1]+'\t'+ch[2]+'\t'+ch[3]+'\t'+ch[4]+'\t'+ch[5];

        //Write String data to dataArray
        for (String s:
                headers) {
            dataArray.add(s);
        }
        //Write Data
        for (MeasData md:
                measList) {

            dataArray.add(String.format(Locale.CANADA,"%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f",
                    md.getValue(MeasData.TIME_STAMP),
                    md.getValue(MeasData.CALC_TEMPERATURE),
                    md.getValue(MeasData.CALC_PH),
                    md.getValue(MeasData.CALC_CL),
                    md.getValue(MeasData.RAW_VOLTAGE),
                    md.getValue(MeasData.RAW_CURRENT)));
        }
        return dataArray;
    }

    private String formFileName(){
        String filename = "wqm_results_" + new SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.CANADA).format(new Date()) + ".tsv";
        Log.i(TAG, "formFileName: filename; " + filename);
        return filename;
    }

}
