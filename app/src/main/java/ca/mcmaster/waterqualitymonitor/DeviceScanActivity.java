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

package ca.mcmaster.waterqualitymonitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity {
    private static final String TAG = DeviceScanActivity.class.getSimpleName();
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_RESPONSE = 2;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static final int REQUEST_ENABLE_LOC = 10;

    private ListView listView;

    private Button btnDbg; //todo debug
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicescan);
        getSupportActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        listView = (ListView) findViewById(R.id.deviceList);

        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                        Log.w("BleActivity", "Coarse location access not granted!");
                        ActivityCompat.requestPermissions(this,
                                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                PERMISSION_RESPONSE);
                            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Fine location access not granted!");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_RESPONSE);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w("BleActivity", "Fine location access not granted!");
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSION_RESPONSE);
            }

        }


        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        // Ensures Location is enabled on the device.  If Location is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!isLocationEnabled(this)) {
                            Log.i(TAG, "Location not enabled");
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(DeviceScanActivity.this);
            dialog.setMessage("Location services are required for this application and not enabled on this device. Please enable Location services to proceed");
            dialog.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(myIntent, REQUEST_ENABLE_LOC);
                    //get gps
                }
            });
            dialog.setNegativeButton("Exit", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    finish();

                }
            });
            dialog.show();

        }


        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);

        //List View listener, connect to target device
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (mScanning)
                    scanLeDevice(false);
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(i);
                if (device == null) return;

                final Intent intent = new Intent(DeviceScanActivity.this,MeasurementActivity.class);
                intent.putExtra(MeasurementActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(MeasurementActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

                startActivity(intent);

            }
        });

        //todo debug button
        btnDbg = (Button) findViewById(R.id.btnDebug);
        //btnDbg.setVisibility(View.GONE);
        btnDbg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mScanning)
                    scanLeDevice(false);

                final Intent intent = new Intent(DeviceScanActivity.this,MeasurementActivity.class);
                intent.putExtra(MeasurementActivity.EXTRAS_DEVICE_NAME, "TEST");
                intent.putExtra(MeasurementActivity.EXTRAS_DEVICE_ADDRESS, "TEST_ADD");

                startActivity(intent);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.menu_settings).setVisible(true);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_progress).setVisible(false);
            menu.findItem(R.id.menu_progress).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_progress).setActionView(
                    R.layout.actionbar_indeterminate_progress);
            menu.findItem(R.id.menu_progress).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_settings:
                Intent i = new Intent(this, Prefs.class);
                startActivity(i);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: here");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        // User chose not to enable Location.
        if (requestCode == REQUEST_ENABLE_LOC && !isLocationEnabled(this)) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        //check if mBluetoothLeScanner has not been initialized
        if (mBluetoothAdapter.isEnabled() && mBluetoothLeScanner == null)
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if(mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        mBluetoothLeScanner.stopScan(mLeScanCallback); //todo repackage into custom method
                        invalidateOptionsMenu();
                    }
                }, SCAN_PERIOD);

                mScanning = true;
                //Settings and filters can be applied through ScanSettings and ScanFilter
                mBluetoothLeScanner.startScan(mLeScanCallback);
            } else {
                mScanning = false;
                mBluetoothLeScanner.stopScan(mLeScanCallback);
            }
            invalidateOptionsMenu();
        } else if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.e(TAG, "scanLeDevice: error: scanner not initialized");
            finish();
        }
    }




    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;

    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                notifyDataSetChanged();
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {

            mLeDevices.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if(result.getDevice().getName()!=null) {
                mLeDeviceListAdapter.addDevice(result.getDevice());
                //mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.d(TAG, "already started"); break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.d(TAG, "cannot be registered"); break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.d(TAG, "power optimized scan not supported"); break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.d(TAG, "internal error"); break;
            }
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}