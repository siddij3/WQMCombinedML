package ca.mcmaster.waterqualitymonitorsuite;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by DK on 2017-11-06.
 *
 * Class to export data to local storage directory
 * Can only be instantiated with 2 parameter constructor
 * for data and destination filename.
 *
 * Output File is created and written to immediately
 * after instantiation
 */

class LocalExport {

    private static final String TAG = LocalExport.class.getSimpleName();
    private static final String DEF_DIR_PATH = "/WQM_Records";

    public static final int STATUS_STARTED = 1;
    public static final int STATUS_COMPLETE = 2;
    public static final int STATUS_FAILED = -1;

    private int writeStatus;

    //Local File
    private File path;
    private File file;
    private OutputStream os;

    LocalExport(String filename, ArrayList<String> dataArray){
        writeStatus = STATUS_STARTED;
        if(initFile(filename)) {
            if (writeData(dataArray))
                writeStatus = STATUS_COMPLETE;
        } else {
            writeStatus = STATUS_FAILED;
        }
    }
    private boolean writeData(ArrayList<String> data){
        boolean success = true;
        for(String str: data) {
            //write Line to file using write method
            success = write(str);
            //If previous write was not successful break loop
            if(!success)
                break;
        }
        close();
        return success;
    }


    private boolean write(String s){
        try {
            os.write(s.getBytes());
            os.write('\n');
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "write: Exception occurred: "+e.toString());
            return false;
        }
    }
    private void close(){
        try {
            if(os != null)
                os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private boolean initFile(String fn){
        path = Environment.getExternalStoragePublicDirectory(DEF_DIR_PATH);
        file = new File(path, fn);
        try {
            path.mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "initFile: Exception occurred creating file: " + e.toString());
            e.printStackTrace();
        }

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && file.exists()){
            //read and write
            try {
                os = new FileOutputStream(file);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "initFile: Exception occurred: "+e.toString());
                e.printStackTrace();
                return false;
            }
        } else if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.e(TAG, "initFile: media not mounted, cannot initialize");
            return false;
        } else {
            Log.e(TAG, "initFile: File does not exist/could not be created");
            return false;
        }
    }

    public String getFileNameAndPath(){
        if (file.exists() && path.exists()){
            return path.getName()+"/"+file.getName();
        } else {
            return "Error, no filepath found";
        }
    }
    public int getWriteCompleteStatus(){ return writeStatus; }

}
