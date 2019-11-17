package ca.mcmaster.potentiostat;

/*
Class to implement BLE communications
replaces output buffer in PC application
and provides the normally associated methods
*/


public class BLE_output {

    ExpSelectActivity activity;

    public BLE_output(ExpSelectActivity activity){
        this.activity = activity;
    }

    public void flush(){

   }

   public void write(char c){
       // TODO: 2017-11-30
       //activity.onSendCommand(String.valueOf(c));
   }
}
