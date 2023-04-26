package ca.mcmaster.potentiostat;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ca.mcmaster.testsuitecommon.GattAttributes;
import ca.mcmaster.testsuitecommon.BluetoothLeService;

import ca.mcmaster.waterqualitymonitorsuite.R;



public class ExpSelectActivity extends AppCompatActivity {
    private static final String TAG = ExpSelectActivity.class.getSimpleName();
    List<Experiment> experimentList =  new ArrayList<>();

    //Extras
    public static final String EXTRAS_EDIT_MODE = "EDIT_MODE";
    public static final String EXTRAS_DEMO_MODE = "DEMO_MODE";
    public static final String EXTRAS_EXP_TYPE = "EXP_TYPE";
    public static final String EXTRAS_EXP_NAME = "EXP_NAME";

    //BLE
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

    //GUI elements
    private TextView dataField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experiment);

        initExperimentList();
        dataField = (TextView) findViewById(R.id.output_data);
        //Set spinner items
        List<String> spinnerArray =  new ArrayList<>();
        initSpinnerArray(spinnerArray);


        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner sItems = (Spinner) findViewById(R.id.spinnerExperiment);
        sItems.setAdapter(adapter);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.title_activity_experiment);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            Log.e(TAG, "onCreate: Action support bar should not be null");
            finish();
        }

        //Run Experiment button and click listener
        Button btnRunExperiment;
        btnRunExperiment = findViewById(R.id.btnRunExperiment);
        btnRunExperiment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selection = sItems.getSelectedItem().toString();
                if(!selection.equals(getResources().getString(R.string.void_selection))) {
                    Experiment selExp = getExpFromList(selection);
                    if(selExp!=null) {
                        final Intent intent = new Intent(ExpSelectActivity.this, ResultsActivity.class);
                        intent.putExtra(EXTRAS_DEMO_MODE, "true"); //todo update?
                        intent.putExtra(EXTRAS_EXP_NAME, selection);
                        intent.putExtra(EXTRAS_EXP_TYPE, selExp.getTypeString());
                        startActivity(intent);
                    } else {
                        Log.e(TAG, "onClick: Selected experiment should not be null!");
                        finish();
                    }
                }
            }
        });

        //New Experiment button and click listener
        Button btnNewExperiment;
        btnNewExperiment = findViewById(R.id.btnCreateNewExperiment);
        btnNewExperiment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                        final Intent intent = new Intent(ExpSelectActivity.this, ExpEditActivity.class);
                        intent.putExtra(EXTRAS_EDIT_MODE, false);
                        intent.putExtra(EXTRAS_EXP_NAME, (String)null);
                        startActivity(intent);

                    }

        });

        //Edit Experiment button and click listener
        Button btnEditExperiment;
        btnEditExperiment = findViewById(R.id.btnEditSelectedExp);
        btnEditExperiment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selection = sItems.getSelectedItem().toString();
                if(!selection.equals(getResources().getString(R.string.void_selection))) {
                    Experiment selExp = getExpFromList(selection);
                    if(selExp!=null) {
                        final Intent intent = new Intent(ExpSelectActivity.this, ExpEditActivity.class);
                        intent.putExtra(EXTRAS_EDIT_MODE, true);
                        intent.putExtra(EXTRAS_EXP_NAME, selection);
                        startActivity(intent);
                    } else {
                        Log.e(TAG, "onClick: Selected experiment should not be null!");
                        finish();
                    }
                }
            }
        });

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // TODO: 2017-11-30 update to grab saved experiments from file/preference
    private void initExperimentList(){
        experimentList.clear();
        Experiment exp1, exp2, exp3, exp4;
        exp1 = new Experiment("Acetaminophen, LSV", Experiment.EXP_CV);
        exp1.setCmds(new String[]{"EA2 72 2 ","EG2 0 ","ED0 0 32768 32768 0 500 1 50 100 50 "});
        exp2 = new Experiment("acetaminophen(III), DPV", Experiment.EXP_DPV);
        exp2.setCmds(new String[]{"EA2 72 2 ","EG2 0 ","EC0 0 32768 32768 100 -100 0 2 10 "});
        exp4 = new Experiment("potassium hexacyanoferrate(III), DPV", Experiment.EXP_DPV);
        exp4.setCmds(new String[]{"EA2 72 2 ","EG2 0 ","ED0 0 32768 32768 0 500 1 50 100 50 "});
        exp3 = new Experiment("potassium hexacyanoferrate(III), CV", Experiment.EXP_CV);
        exp3.setCmds(new String[]{"EA2 72 2 ","EG2 0 ","EC0 0 32768 32768 100 -100 0 2 10 "});

        experimentList.add(exp1);
        experimentList.add(exp2);
        experimentList.add(exp3);
        experimentList.add(exp4);
    }

    public Experiment getExpFromList(String name){
        for (int i = 0; i < experimentList.size(); i++) {
            if(experimentList.get(i).getName().equals(name))
                return experimentList.get(i);
        }
        return null;
    }

    private void initSpinnerArray(List<String> sList){
        sList.clear();
        sList.add(getResources().getString(R.string.void_selection));
        for (Experiment e:
             experimentList) {
            sList.add(e.getName());
        }


    }

    void showMessage(String s){
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
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
                if (enableWrite){
                    //implement write action here
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                String s = new String(data);
                Log.d(TAG, "onReceive: " + s);

                //add data to form string, if NL encountered parse and update data
                for (byte b:
                        data) {
                   //TODO data received action
                }
            }
        }
    };
    


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

    private void setActionBarSubtitle(){
        if(getSupportActionBar()!=null){
            if(BLE_Connected){
                // TODO: 2017-11-30 update 
                /*if(measuring)
                    getSupportActionBar().setSubtitle(getResources().getString(R.string.connected_measuring));
                else
                    getSupportActionBar().setSubtitle(getResources().getString(R.string.connected_stopped));
            } else {
                getSupportActionBar().setSubtitle(getResources().getString(R.string.disconnected));*/
            }

        } else {
            Log.e(TAG, "setActionBarSubtitle: Should not be null!");
            finish();
        }
    }

    public void writeDataOutput(String s){
        //todo update
        displayData(s);

    }

    public void displayData(final String data) {
        if (data != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String s = dataField.getText().toString() +"\n"+ data;
                    dataField.setText(s);
                }
            });
        }
    }

}
