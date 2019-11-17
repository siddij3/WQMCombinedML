package ca.mcmaster.potentiostat;

/*
Class to implement BLE communications
replaces input buffer in PC application
and provides the normally associated methods
*/

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class BLE_input {
    private static final String TAG = "BLE_input";
    boolean markEnabled = false;
    private int maxMarkVal = 0;
    public List<Byte> readBuffer = new ArrayList<Byte>();
    private Lock lock = new Lock();

    int rPtr = 0;
    int wPtr = 0;


    public int read(){
        //Log.d(TAG, "Starting read: wPtr: " + wPtr + " rPtr: " + rPtr + " list len: " + readBuffer.size() + " marked: " + markEnabled);
        int r;

        try {
            lock.lock();

            if (rPtr < readBuffer.size()) {
                try {

                    if (markEnabled) {
                        //mark is enabled, do not remove read element and increment rPtr
                        r = readBuffer.get(rPtr);
                        rPtr++;
                        //Log.d(TAG, "Finishing read: wPtr: " + wPtr + " rPtr: " + rPtr + " list len: " + readBuffer.size() + " marked: " + markEnabled);
                    } else {
                        //mark is disabled, read and remove element, decrement wPtr
                        if (rPtr != 0) {
                            //rPtr should always be 0 if mark is not enabled
                            Log.d(TAG, "read: rPtr value not valid, pointer was not properly reset");
                            return -1;
                        }
                        r = readBuffer.get(rPtr);
                        readBuffer.remove(rPtr);

                        //Log.d(TAG, "Finishing read: wPtr: " + wPtr + " rPtr: " + rPtr + " list len: " + readBuffer.size() + " marked: " + markEnabled);
                    }
                    if (markEnabled && (rPtr >= maxMarkVal)) {
                        //Log.d(TAG, "read: Disabling mark, exceeded mark limit "+maxMarkVal);
                        clearMarked();
                        disableMark();

                    }
                    return r;
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "read: Exception occurred:" + e.toString() + ")");
                    Log.d(TAG, "wPtr: " + wPtr + " rPtr: " + rPtr + " list len: " + readBuffer.size() + " marked: " + markEnabled);

                    return -1;
                } finally {
                    lock.unlock();
                }

            } else {
                //no more data available, cannot read
                return -1;
            }
        } catch (Exception e){
            Log.d(TAG, "read: Exception occurred:" + e.toString() + ")");
            return -1;
        } finally {
            lock.unlock();
        }
    }

    public void writeBuffer(byte b){
        try {
            lock.lock();
            readBuffer.add(b);
            //wPtr++;
        } catch (Exception e){
            Log.d(TAG, "writeBuffer: Exception occurred:" + e.toString() + ")");
        } finally {
            lock.unlock();
        }

    }

    public int read(byte b[], int off, int len){
        int r;
        if (off != 0){
            Log.d(TAG, "read: Error: read does not currently implement offset functionality");
            return -1;
        }

        for (int i = 0; i < len ;i++){
            r = read();
            if (r == -1)
               return r;
            b[i] = (byte)r;
        }
        return 0; //todo implement conditional int returns similar to input buffer read method
    }

    public void mark(int m){
        Log.d(TAG, "mark: Enabled: "+m);;
        maxMarkVal = m;
        markEnabled = true;
    }

    public void reset(){
        Log.d(TAG, "mark: Reset ");;
        if(markEnabled){
            rPtr = 0;
            disableMark();
        }
    }

    public int available(){
        return readBuffer.size()-rPtr;
    }

    //removes mark
    private void disableMark(){
        maxMarkVal = 0;
        markEnabled = false;
        Log.d(TAG, "disableMark: disabled");

    }

    private void clearMarked(){
        for (int i = 0; i < rPtr; i++) {
            readBuffer.remove(0);
        }
        rPtr = 0;
    }


    public void printBuffer(){
        try{
            lock.lock();
            for (byte b:
                    readBuffer) {
                Log.d(TAG, "printBuffer - byteVal: "+ b);

            }
        } catch (Exception e){
            Log.d(TAG, "printBuffer: Exception occurred:" + e.toString() + ")");
        } finally {
            lock.unlock();
        }

    }

}
