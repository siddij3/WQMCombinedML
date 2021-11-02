package ca.mcmaster.testsuitecommon;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ca.mcmaster.waterqualitymonitorsuite.R;

/**
 * Created by DK on 2017-11-30.
 */

public class Common {
    private static final String TAG = Common.class.getSimpleName();

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    Context context;
    public Common(Context context){
        this.context = context;
    }

    /*// Iterates through the supported GATT Services/Characteristics.
    void detectGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        String unknownServiceString = context.getResources().getString(R.string.unknown_service);
        String unknownCharaString = context.getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        //gattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

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
                    Log.d(TAG,uuid + " has writD/DeviceScanActivity: onResume: here
D/DeviceScanActivity: onResume: here
W/BleActivity: Fine location access not granted!
D/DeviceScanActivity: onResume: heree characteristic");
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

    }*/
}
