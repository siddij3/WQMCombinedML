package ca.mcmaster.potentiostat;

import android.util.Log;

/**
 * Created by DK on 2017-10-19.
 */

public class InterfaceToUI {

    private static final String TAG = "Display";

    //Determines priority level of messages to be displayed, 0 = display all
    private final static int MIN_DISPLAY_PRIORITY = 0;
    final static int P_DBG = 0;
    final static int P_LOW = 1;
    final static int P_MED = 2;
    final static int P_HIGH = 3;
    final static int P_USER = 4;

    public void dbgOut (String output){
        Log.d(TAG, "dbgOut: " + output);
    }
    //Display information test based on priority, higher value = higher priority
    public void infoOut (String output, int priority){
        Log.d(TAG, "infoOut: " + output);
    }
}
