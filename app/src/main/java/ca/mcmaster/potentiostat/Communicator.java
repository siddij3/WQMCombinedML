package ca.mcmaster.potentiostat;


import android.util.Log;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;


// TODO: 2017-10-19 Adjust for BLE usage
// TODO: 2017-10-21 sync results with cloud
// TODO: 2017-10-21 update settings

// Old Java Todos
// TODO: 2017-09-15 DPV experiment
// TODO: 2017-09-15 finalize valid parameters
// TODO: 2017-09-15 Experiment analysis
// TODO: 2017-09-15 Add experiment graphics
// TODO: 2017-09-20 Add gain trim 

public class Communicator
{
    private static final String TAG = "experiment";

    ExpSelectActivity activity = null;

    //ResultsPlot object
    ResultsPlot dataPlot = null;

    //Settings object
    Settings settings = null;

    //for containing the ports that will be found
    private Enumeration ports = null;

    //passed from main GUI
    InterfaceToUI display = null;

    //map the port names to CommPortIdentifiers
    private HashMap portMap = new HashMap();

    //todo from java pc version. update classes to seperate comm peripherals into seperate classes
/*    //this is the object that contains the opened port
    private CommPortIdentifier selectedPortIdentifier = null;
    private SerialPort serialPort = null;*/
/*
 //-------------------------PC application uses input/output buffers---------------------
    //input and output streams for sending and receiving data over serial comms
    private BufferedInputStream input = null;
    private OutputStream output = null;*/

     //------------------Android application uses classes replicate methods----------------
    //input and output classes for sending and receiving data over BLE
    private BLE_input input;
    private BLE_output output;

    //boolean flag indicating if serial connection has been made
    private boolean bConnected = false;

    private boolean bAborted = false;

    //Read buffer array
    private byte[] readBuffer = new byte[1024];

    List<String> readLines = new ArrayList<>();
    List<byte[]> readBytes = new ArrayList<>();
    List<Byte> byteList = new ArrayList<>();
    List<Character[]> readChars = new ArrayList<>();


    private String readString = "";
    private String message = "";

    //flag for communication status
    private int HSstatus = 0;

    final static int EXP_START = 33;
    final static int EXP_INPROC = 23;
    final static int EXP_DONE = 3;
    final static int EXP_CMD_SENT = 32;
    final static int EXP_CMD_INPROC = 22;
    final static int EXP_CMD_RECD = 2;
    final static int INIT_CMD_SENT = 31;
    final static int INIT_CMD_INPROC = 21;
    final static int INIT_CMD_RECD = 1;
    final static int NONE = 0;

    //store available bytes for use by other methods:
    private int availableBytes;

    //the timeout value for connecting with the port
    final static int TIMEOUT = 2000;

    //some ascii values for for certain things
    final static int SPACE_ASCII = 32;
    final static int C_ASCII = 67;
    final static int B_ASCII = 66;
    final static int NL_ASCII = 10;
    final static int CR_ASCII = 13;

    //a string for recording what goes on in the program
    //this string is written to the GUI
    String logText = "";

    public Communicator(InterfaceToUI display, Settings settings, ResultsPlot dataPlot, BLE_input input, BLE_output output, ExpSelectActivity activity)
    {
        this.display = display;
        this.settings = settings;
        this.dataPlot = dataPlot;
        this.input = input;
        this.output = output;
        this.activity = activity;
    }
/* todo update or remove
    //search for all the serial ports
    //pre: none
    //post: adds all the found ports to a combo box on the GUI
    public void searchForPorts()
    {
        ports = CommPortIdentifier.getPortIdentifiers();
        display.cboxPorts.removeAllItems();
        while (ports.hasMoreElements())
        {
            CommPortIdentifier curPort = (CommPortIdentifier)ports.nextElement();

            //get only serial ports
            if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL)
            {
                display.cboxPorts.addItem(curPort.getName());
                portMap.put(curPort.getName(), curPort);
            }
        }
    }

    //connect to the selected port in the combo box
    //pre: ports are already found by using the searchForPorts method
    //post: the connected comm port is stored in commPort, otherwise,
    //an exception is generated
    public boolean connect()
    {
        boolean successful = false;
        int baudRate = 19200;
        String selectedPort = (String) display.cboxPorts.getSelectedItem();
        selectedPortIdentifier = (CommPortIdentifier)portMap.get(selectedPort);

        CommPort commPort = null;

        try
        {
            //the method below returns an object of type CommPort
            commPort = selectedPortIdentifier.open("Communicator", TIMEOUT);
            //the CommPort object can be casted to a SerialPort object
            serialPort = (SerialPort)commPort;

            display.infoOut("Connected to port: " + commPort, InterfaceToUI.P_LOW);

        serialPort.setSerialPortParams(
                        baudRate,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

            display.infoOut("Baud Rate:"+serialPort.getBaudRate(),P_LOW);
            display.infoOut("Data Bits:"+serialPort.getDataBits(),P_LOW);
            display.infoOut("Stop Bits:"+serialPort.getStopBits(),P_LOW);
            display.infoOut("Parity:"+serialPort.getParity(),P_LOW);
            display.infoOut("Flow Control:"+serialPort.getFlowControlMode(),P_LOW);

            successful = true;
            return successful;
        }
        catch (PortInUseException e)
        {
            display.dbgOut(selectedPort + " is in use. (" + e.toString() + ")");
            return successful;
        }
        catch (Exception e)
        {
            display.dbgOut("Failed to open " + selectedPort + "(" + e.toString() + ")");
            return successful;
        }
    }*/

/*    //open the input and output streams
    //pre: an open port
    //post: initialized input and output streams for use to communicate data
    public boolean initIOStream()
    {
        //return value for whether opening the streams is successful or not
        boolean successful = false;
        try {
            input = new BufferedInputStream();
            output = serialPort.getOutputStream();

            successful = true;
            return successful;
        }
        catch (IOException e) {
            display.dbgOut("I/O Streams failed to open. (" + e.toString() + ")");
            return successful;
        }

    }*/

/*
    //disconnect the serial port
    //pre: an open serial port
    //post: closed serial port
    public void disconnect()
    {
        //close the serial port
        try
        {
            serialPort.removeEventListener();
            serialPort.close();
            input.close();
            output.close();
            setConnected(false);
            display.toggleControls();
            display.infoOut("Disconnected",InterfaceToUI.P_HIGH);
            display.txtLog.setForeground(Color.gray);
        }
        catch (Exception e)
        {
            display.dbgOut("Failed to close port:" + "(" + e.toString() + ")");
        }
    }

    final public boolean getConnected()
    {
        return bConnected;
    }

    public void setConnected(boolean bConnected)
    {
        this.bConnected = bConnected;
    }*/

    //Clear read buffers
    public void clrReadBuffers(){
        readBytes.clear();
        readLines.clear();
        byteList.clear();
    }

    public boolean runExperiment(String tempCmd[], int dt){
        long time = System.currentTimeMillis();
        display.infoOut("Starting experiment",InterfaceToUI.P_MED);
        String[] cmd = new String[3];
        boolean success = false;
        //todo boolean retain = display.cbRetainResults.getState();
        boolean retain = false;

        //Clear plot dataset if retain option not selected:
        if (!retain)
            dataPlot.clearSeries();

        for (int i = 0; i <= 2; i++) {
            //cmd[i] = settings.getCommand(i + 1); todo temporarily bypassed
            cmd[i] = tempCmd[i];
            if (cmd[i] == null || cmd[i].isEmpty()){
                success = false;
                break;
            }
            //Set domain axis, run only once
            if (i==2)
                dataPlot.setDomainAxis(settings.getMinFromParams(),settings.getMaxFromParams(),retain);

            //Valid command array formed, clear read data and send cmd
            clrReadData();
            settings.printParams();

            sendCommand(cmd[i]);
            if (i==2)
                activity.writeDataOutput("mV       A");
                waitTime(3000);
            if(i!=3) {//temp todo
                if (inputHandler(2, dataPlot.getSeriesCount(),dt)) {
                    success = true;
                } else {
                    success = false;
                    break;
                }
            } else {
/*                Log.d(TAG, "runExperiment: sleeping");
                waitTime(3000);
                input.printBuffer();*/
            }
        }
        if (bAborted) {
            display.dbgOut("Experiment aborted!");
            bAborted = false;
        } else if (success){
            display.infoOut("Experiment completed successfully",InterfaceToUI.P_MED);

        }

        else
            display.infoOut("Experiment failed",InterfaceToUI.P_MED);

        return success;
    }

    //Handles incoming data during experiment
    //Discards supplied number of initial measurements
    //Method returns true when "no" received, else runs indefinitely
    private boolean inputHandler(int discards, int scans, int dt){
        Log.d(TAG, "inputHandler: enter");
        while(true){
            try {
                while(input.available()==0){}
                Log.d(TAG, "inputHandler: available bytes: "+input.available());
                waitTime(50);
                String line = formReadLine();
                byte databytes[] = new byte[10];
                MeasData dataPnt = new MeasData(3000);
                //int dataType = settings.getDataType();
                int dataType = dt; //todo temp

                //check if full line formed
                if (line == null) {

                } else if(line.equals("EXCEPTION")){ //Exception occurred in formReadLine method
                    return false;
                } else if(line.equals("a")){ //experiment aborted
                    display.dbgOut("Experiment aborted");
                } else if (line.equals("B")) { //data line follows
                    Log.d(TAG, "inputHandler: 'B' read, starting data read");
                    if (dataType == 1) {

                        input.read(databytes, 0, 6);
                        dataPnt = parseData(databytes, dataType, 3000);// TODO: 2017-09-20 add variable gain
                        activity.writeDataOutput(String.format("%.2f  %.3E",dataPnt.voltage,dataPnt.current));
                        Log.d(TAG, "inputHandler: " + dataPnt.toString());
                    } else if (dataType == 2) {
                        input.read(databytes, 0, 10);
                        dataPnt = parseData(databytes, dataType, 3000);// TODO: 2017-09-20 add variable gain
                        Log.d(TAG, "inputHandler: " + dataPnt.toString());
                    }
                    if ((dataType == 1)||(dataType == 2)){

                        if (discards > 0)
                            discards--;
                        else {
                            //todo dataPlot.addPoint(scans,dataPnt.voltage, dataPnt.current);
                            //display.infoOut(dataPnt.toString(), InterfaceToUI.P_LOW);
                        }
                    }
                } else if (line.equals("no")) { //end of experiment data
                    display.infoOut(line, InterfaceToUI.P_LOW);
                    return true;
                } else if (line.equals("S")) { //new scan/series
                    scans++;
                    display.infoOut("Scans: " + scans, InterfaceToUI.P_LOW);

                } else {
                    //Log.wtf("dont care","Should not be here");
                }
            } catch(Exception e) {
                display.dbgOut("Input Handler exception occurred: (" + e.toString() + ")");
                return false;
            }

        }
        
    }

    public MeasData parseData(byte[] bytes, int dataType, int gain){
        MeasData d = new MeasData(gain);
        if (dataType == 1) {
            d.setData(uint16(bytes, 0), int32(bytes, 2));
        } else if (dataType == 2) {
            d.setData(uint16(bytes, 0), int32(bytes, 2) - int32(bytes, 6)); //current = forward - reverse
        }
        return d;
    }

    private int uint16(byte[] arr, int start){
        int result, result2;
        int a;
        int b;
        if ((arr.length - start)>=2){
/*            a = (int)arr[start];
            a = a << 8;
            b = (int)arr[start+1];
            result = a + b;*/
            result = (arr[start+1] & 0xFF) << 8 | (arr[start] & 0xFF);
            //System.out.println(result);
        } else {
            result = -1;
        }
        return result;
    }

    private int int32(byte[] arr, int start){
        int result;

        if ((arr.length - start)>=4){
            result = arr[start+3] << 24 | (arr[start + 2] & 0xFF) << 16 | (arr[start + 1] & 0xFF) << 8 | (arr[start] & 0xFF);
        } else {
            result = 0;
        }
        return result;
    }
    //Returns next serial line
    //If no complete line return null
    //Treats consecutive CR/NL as one
    private String formReadLine(){
        Log.d(TAG, "formReadLine: entering");
        String s = "";
        boolean lineformed = false;
        byte b;

        try{
            input.mark(input.available());
            while (input.available()>0 && !lineformed) {
                b = (byte)input.read();

                if ((b == NL_ASCII) || (b == CR_ASCII)){
                    //input.mark(2);
                    //if next character is not \n or \r reset to mark
                    //b = (byte)input.read();
                    //if ((b != NL_ASCII) && (b != CR_ASCII))
                       //input.reset();
                    lineformed = true;
                    Log.d(TAG, "formReadLine: ------------------- Line formed: String s = "+s);
                } else {
                    s += (char)b;
                    //Log.d(TAG, "formReadLine: String s = "+s);
                }
            }
            if (!lineformed){
                s = null;
                input.reset();
            }
            return s;
        } catch (Exception e){
            display.dbgOut("Failed to form read line. (" + e.toString() + ")");
            return "EXCEPTION";
        }

    }

    public void sendCommand(String cmd){
        if (sendInit()) {
            display.infoOut("Sending command: " + cmd, InterfaceToUI.P_DBG);

            sendString(cmd);


        }
    }

    public boolean sendInit()
    {
        try
        {
            int i = 0;
            byte b;
            display.infoOut("sendInit(), writing initial !", InterfaceToUI.P_DBG);

            Thread.sleep(200);

            Log.d(TAG, "sendInit: clearing input stream " + input.available() );
            //clear any data in input stream
            clrReadData();
            Log.d(TAG, "sendInit: cleared input stream " + input.available() );

            //send initialize cmd
            output.write('!');
            output.flush();

            // wait until data is rec'd

            while(input.available()==0);
            Thread.sleep(100);

            display.infoOut("input avail: " + input.available(), InterfaceToUI.P_DBG);
            while (((b = readByte()) != C_ASCII) && (i<=10)){

                //display.infoOut("Received Val: " + String.valueOf((int)b), InterfaceToUI.P_DBG);
                display.infoOut("sendInit(), writing !, i = " + i, InterfaceToUI.P_DBG);
                clrReadData();
                display.infoOut("input avail: " + input.available(), InterfaceToUI.P_DBG);
                output.write('!');
                while(input.available()==0);
                Thread.sleep(100);
                i++;
            }

            //Check if reply received or if max attempts exceeded
            if (b == C_ASCII){
                display.infoOut("sendInit True", InterfaceToUI.P_DBG);
                clrReadData();
                return true;
            }
            else{
                display.infoOut("sendInit False", InterfaceToUI.P_DBG);
                clrReadData();
                return false;
            }
        }
        catch (Exception e)
        {
            display.dbgOut("Failed to write data. (" + e.toString() + ")");
            return false;
        }
    }
    //read all bytes left in input stream
    public void clrReadData(){
        try {
            Log.d(TAG, "clrReadData: clearing read data");
            int i = input.available();
            byte[] b = new byte[i];
            input.read(b, 0, i);
        }catch (Exception e) {
            display.dbgOut("Failed to read data. (" + e.toString() + ")");
        }
    }

    //read single byte from serial inputStream
    public byte readByte(){
        byte b = 0;
        try {
            b = (byte)input.read();
            return b;
        } catch (Exception e) {
            display.dbgOut("Failed to read data. (" + e.toString() + ")");
            return b;
        }
    }
    //wait until availableBytes == 0 then set amount of time
    public void waitRead(int delay)
    {
        try
        {
            while(input.available()>0);
            Thread.sleep(delay);
        }
        catch (Exception e)
        {
            display.dbgOut("Exception occurred: (" + e.toString() + ")");
        }
    }

    //wait for specified amount of time (ms)
    public void waitTime(int delay)
    {
        try
        {
            Thread.sleep(delay);
        }
        catch (Exception e)
        {
            display.dbgOut("Exception occurred: (" + e.toString() + ")");
        }
    }
    public void sendString(String strOutput)
    {
        try
        {
            display.infoOut("sendString():" + strOutput, InterfaceToUI.P_DBG);
            for (int i = 0; i < strOutput.length(); i++) {
                output.write(strOutput.charAt(i));
                output.flush();
                Thread.sleep(50);
                }
        }
        catch (Exception e)
        {
            display.dbgOut("Failed to write data. (" + e.toString() + ")");
        }
    }

    void setAborted(boolean b){
        bAborted = b;
    }
    boolean getAborted(){
        return bAborted;
    }

}
