package ca.mcmaster.potentiostat;

/**
 * Created by DK on 2017-10-21.
 */

public class Lock {
    private static final String TAG = "Lock";
    public boolean isLocked = false;

    public synchronized void lock()
            throws InterruptedException {
        //Log.d(TAG, "lock: Locking");
        while(isLocked){
            wait();
        }
        isLocked = true;
        //Log.d(TAG, "lock: Locked");
    }

    public synchronized void unlock(){
        isLocked = false;
        //Log.d(TAG, "lock: Unlocked");
        notify();
    }
}

