package ca.mcmaster.potentiostat;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by DK on 2017-06-03.
 */
public class Settings {

    //Constants for experiment parameters
    final static String CVPARAM[] = {
            "CV: Cleaning Potential",   //0
            "CV: Cleaning Time",        //1
            "CV: Deposition Potential", //2
            "CV: Deposition Time",      //3
            "CV: Vertex 1",             //4
            "CV: Vertex 2",             //5
            "CV: Start",                //6
            "CV: Scans",                //7
            "CV: Slope",                //8
    };

    final static String LSVPARAM[] = {
            "LSV: Cleaning Potential",   //0
            "LSV: Cleaning Time",        //1
            "LSV: Deposition Potential", //2
            "LSV: Deposition Time",      //3
            "LSV: Start",                //4
            "LSV: Stop",                 //5
            "LSV: Slope",                //6
    };

    final static String DPVPARAM[] = {
            "DPV: Cleaning Potential",   //0
            "DPV: Cleaning Time",        //1
            "DPV: Deposition Potential", //2
            "DPV: Deposition Time",      //3
            "DPV: Start",                //4
            "DPV: Stop",                 //5
            "DPV: Step",                 //6
            "DPV: Pulse Height",         //7
            "DPV: Pulse Period",         //8
            "DPV: Pulse Width",          //9
    };//TODO fix for dpv

    //HashMaps for storing parameters
    public HashMap<String, String> params = new HashMap<>();

    ArrayList<String> strCommand = new ArrayList<>();

    //Data type for parsing received data
    private int dataType = 0;

    //Stores max and min x axis values for selected experiment
    public double xmin = 0.0;
    public double xmax = 1.0;

    //passed from main GUI
    InterfaceToUI display = null;

    //Constructor
    public Settings(InterfaceToUI display){
        this.display = display;
    }
    
    //Methods
    public void printParams () {
        //todo params.forEach((k,v)-> display.infoOut(k+"\t"+v,InterfaceToUI.P_LOW));
    }


/*    //TODO change to return command array
    public String getCommand(int cmdnum){
        String command = "";
        try
        {
            if (updateSettings()){
                switch (cmdnum){
                    case 1:     //ADC command
                        command = String.format("EA%s %s %s ",getInpBufferCmd(),getSampRateCmd(),getPGASetCmd());
                        break;
                    case 2:     //Gain command
                        command = String.format("EG%s %s ",getGainCmd(),get2ElecCmd());
                        break;
                    case 3:     //Experiment command;
                        command = getExperimentCmd(params.get("Experiment"));
                        break;
                    default:
                        display.dbgOut("Could not get command information, invalid command number");
                        break;
                }
            }
            return command;
        }
        catch (Exception e)
        {
            display.dbgOut("Get command failed. (" + e.toString() + ")");
            return null;
        }
    }*/
    // post: Returns a string with experiment command for selected experiment, returns null if invalid or empty entry found
    public String getExperimentCmd(String selectedExperiment){
        String experimentCmd = "";
        String s[] = new String[30];
        //initialize all to blank
        for (int i = 0; i < s.length; i++) {
            s[i] = "";
        }
        try
        {
            switch (selectedExperiment){
                case "Cyclic Voltammetry":
                    dataType = 1; //uint16+int32
                    display.infoOut("CV",InterfaceToUI.P_DBG);
                    s[0]+= getIntCmd(CVPARAM[0], 9999, 0);
                    s[1]+= getIntCmd(CVPARAM[1], 9999, 0);
                    s[2]+= getIntScaledCmd(CVPARAM[2], 1499, -1500);
                    s[3]+= getIntScaledCmd(CVPARAM[3], 1499, -1500);
                    s[4]+= getIntCmd(CVPARAM[4], 1499, -1500);
                    s[5]+= getIntCmd(CVPARAM[5], 1499, -1500);
                    s[6]+= getIntCmd(CVPARAM[6], 1499, -1500);
                    s[7]+= getIntCmd(CVPARAM[7], 200, 1);
                    s[8]+= getIntCmd(CVPARAM[8], 2000, 1);
                    if (!s[0].isEmpty()&&!s[1].isEmpty()&&!s[2].isEmpty()&&!s[3].isEmpty()&&!s[4].isEmpty()
                            &&!s[5].isEmpty()&&!s[6].isEmpty()&&!s[7].isEmpty()&&!s[8].isEmpty()){
                        experimentCmd = String.format("EC%s %s %s %s %s %s %s %s %s ",s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8]);
                    }
                    break;
                case "Linear Sweep Voltammetry":
                    dataType = 1; //uint16+int32
                    display.infoOut("LSV",InterfaceToUI.P_DBG);
                    s[0]= getIntCmd(LSVPARAM[0], 9999, 0);
                    s[1]= getIntCmd(LSVPARAM[1], 9999, 0);
                    s[2]= getIntScaledCmd(LSVPARAM[2], 1499, -1500);
                    s[3]= getIntScaledCmd(LSVPARAM[3], 1499, -1500);
                    s[4]= getIntCmd(LSVPARAM[4], 1499, -1500);
                    s[5]= getIntCmd(LSVPARAM[5], 1499, -1500);
                    s[6]= getIntCmd(LSVPARAM[6], 2000, 1);
                    if (!s[0].isEmpty()&&!s[1].isEmpty()&&!s[2].isEmpty()&&!s[3].isEmpty()&&!s[4].isEmpty()
                            &&!s[5].isEmpty()&&!s[6].isEmpty()){
                        experimentCmd = String.format("EL%s %s %s %s %s %s %s ",s[0],s[1],s[2],s[3],s[4],s[5],s[6]);
                    }
                    break;
                case "Differential Pulse Voltammetry":
                    dataType = 2; //uint16+int32+int32
                    display.infoOut("DPV",InterfaceToUI.P_DBG);
                    s[0]+= getIntCmd(DPVPARAM[0], 9999, 0);
                    s[1]+= getIntCmd(DPVPARAM[1], 9999, 0);
                    s[2]+= getIntScaledCmd(DPVPARAM[2], 1499, -1500);
                    s[3]+= getIntScaledCmd(DPVPARAM[3], 1499, -1500);
                    s[4]+= getIntCmd(DPVPARAM[4], 1499, -1500);
                    s[5]+= getIntCmd(DPVPARAM[5], 1499, -1500);
                    s[6]+= getIntCmd(DPVPARAM[6], 200, 1);
                    s[7]+= getIntCmd(DPVPARAM[7], 150, 1);
                    s[8]+= getIntCmd(DPVPARAM[8], 1000, 1);
                    s[9]+= getIntCmd(DPVPARAM[9], 1000, 1);
                    if (!s[0].isEmpty()&&!s[1].isEmpty()&&!s[2].isEmpty()&&!s[3].isEmpty()&&!s[4].isEmpty()
                            &&!s[5].isEmpty()&&!s[6].isEmpty()&&!s[7].isEmpty()&&!s[8].isEmpty()&&!s[9].isEmpty()){
                        experimentCmd = String.format("ED%s %s %s %s %s %s %s %s %s %s ",s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8],s[9]);
                    }
                    break;
                default:
                    experimentCmd = "";
                    break;
            }
            display.infoOut("Experiment Command: "+experimentCmd,0);
            return experimentCmd;
        }
        catch (Exception e)
        {
            display.dbgOut("Get experiment command failed. (" + e.toString() + ")");
            return "";
        }
    }

    //// TODO: 2017-10-19 update method for new graph tool. possibly update to move to another class
/*

    //Get X-axis range from selected experiment settings, returns range
    //Does not check if data is valid, this should be done previously
    public Range getRangeFromParams(){
        double upper = 1.0;
        double lower = 0.0;
        double d = 0.0;
        String selectedExperiment = params.get("Experiment");
        //Obtain upper and lower depending on selected experiment
        try
        {
            switch (selectedExperiment){
                case "Cyclic Voltammetry":
                    upper = max(getParamVal(CVPARAM[4]),getParamVal(CVPARAM[5]),getParamVal(CVPARAM[6]));
                    lower = min(getParamVal(CVPARAM[4]),getParamVal(CVPARAM[5]),getParamVal(CVPARAM[6]));
                    break;
                case "Linear Sweep Voltammetry":
                    upper = max(getParamVal(LSVPARAM[4]),getParamVal(LSVPARAM[5]));
                    lower = min(getParamVal(LSVPARAM[4]),getParamVal(LSVPARAM[5]));
                    break;
                case "Differential Pulse Voltammetry":
                    upper = max(getParamVal(DPVPARAM[4]),getParamVal(DPVPARAM[5]));
                    lower = min(getParamVal(DPVPARAM[4]),getParamVal(DPVPARAM[5]));
                    break;
                default:
                    display.dbgOut("Could not obtain range from experiment parameters, experiment not found");
                    break;
            }
            display.infoOut("Range: Lower - "+ lower +" Upper - "+ upper,0);
            //Extend axis an extra 8% each side
            d = upper - lower;
            d = 0.04 * d;
            upper = upper + d;
            lower = lower - d;

            Range range = new Range(lower,upper);
            return range;
        }
        catch (Exception e)
        {
            display.dbgOut("Could not obtain range from experiment parameters, exception occurred: (" + e.toString() + ")");
            Range range = new Range(0.0,1.0);
            return range;
        }


    }
*/
    public double getMinFromParams(){
        // TODO: 2017-10-20
        return -0.5;
    }


    public double getMaxFromParams(){
        // TODO: 2017-10-20
        return 0.5;
    }

    private String getInpBufferCmd(){
        String setting = params.get("Input Buffer");
        String cmd = null;
        if (setting.equals("True")){
            cmd = "2";
        } else if (setting.equals("False")){
            cmd = "0";
        } else {
            //TODO: ADDEXCEPTION
            display.dbgOut("no input buffer command");
        }
        return cmd;
    }
    private String getSampRateCmd(){
        String setting = params.get("Sample Rate");
        //display.dbgOut("samp rate:" + setting);
        String cmd = null;

        switch (setting){
            case "2.5 Hz":
                cmd = "03";
                break;
            case "5 Hz":
                cmd = "13";
                break;
            case "10 Hz":
                cmd = "23";
                break;
            case "15 Hz":
                cmd = "33";
                break;
            case "25 Hz":
                cmd = "43";
                break;
            case "30 Hz":
                cmd = "53";
                break;
            case "50 Hz":
                cmd = "63";
                break;
            case "60 Hz":
                cmd = "72";
                break;
            case "100 Hz":
                cmd = "82";
                break;
            case "500 Hz":
                cmd = "92";
                break;
            case "1 kHz":
                cmd = "A1";
                break;
            case "2 kHz":
                cmd = "B0";
                break;
            case "3.75 kHz":
                cmd = "C0";
                break;
            case "7.5 kHz":
                cmd = "D0";
                break;
            case "15 kHz":
                cmd = "E0";
                break;
            case "30 kHz":
                cmd = "F0";
                break;
            default:
                //TODO: ADDEXCEPTION
                display.dbgOut("invalid sample rate command");
                break;
        }
        return cmd;
    }
    private String get2ElecCmd() {
        String setting = params.get("2 Electrode");
        String cmd = null;
        if (setting.equals("True")) {
            cmd = "1";
        } else if (setting.equals("False")) {
            cmd = "0";
        } else {
            //TODO: ADD EXCEPTION
            display.dbgOut("invalid 2 electrode command");
        }
        return cmd;
    }
    private String getPGASetCmd(){
        String setting = params.get("PGA Setting");
        String cmd = null;

        switch (setting){
            case "1X":
                cmd = "0";
                break;
            case "2X":
                cmd = "1";
                break;
            case "4X":
                cmd = "2";
                break;
            case "8X":
                cmd = "3";
                break;
            case "16X":
                cmd = "4";
                break;
            case "32X":
                cmd = "5";
                break;
            case "64X":
                cmd = "6";
                break;
            default:
                display.dbgOut("invalid PGA command");
                break;
        }
        return cmd;
    }
    private String getGainCmd() {
        String setting = params.get("Gain");
        String cmd = null;

        switch (setting) {
            case "Bypass":
                cmd = "0";
                break;
            case "100 Ω (15 mA FS)":
                cmd = "1";
                break;
            case "3 kΩ (500 uA FS)":
                cmd = "2";
                break;
            case "30 kΩ (50 uA FS)":
                cmd = "3";
                break;
            case "300 kΩ (5 uA FS)":
                cmd = "4";
                break;
            case "3 MΩ (500 nA FS)":
                cmd = "5";
                break;
            case "30 MΩ (50 nA FS)":
                cmd = "6";
                break;
            case "100 MΩ (15 nA FS)":
                cmd = "6";
                break;
            default:
                display.dbgOut("invalid Gain command");
                break;
        }
        return cmd;
    }
    private String getIntCmd(String paramKey, int max, int min){
        String setting = params.get(paramKey);
        int setval = Integer.parseInt(setting);
        String cmd = "";
        if (setting.isEmpty())
            display.infoOut(String.format("Value cannot be empty: %s",paramKey),InterfaceToUI.P_USER);
        else if (setval > max)
            display.infoOut(String.format("Value exceeds maximum: %s (Max = %d)",paramKey,max),InterfaceToUI.P_USER);
        else if (setval < min)
            display.infoOut(String.format("Value is below minimum: %s (Min = %d)",paramKey,min),InterfaceToUI.P_USER);
        else if (setval <= max && setval >= min)
            cmd = Integer.toString(setval);

        return cmd;
    }
    private String getIntScaledCmd(String paramKey, int inHigh, int inLow, int outHigh, int outLow) {
        int setval = Integer.parseInt(params.get(paramKey));
        double scale = (double) (outHigh - outLow) / (inHigh - inLow);
        String cmd = "";

        int inMid = (inHigh + inLow) / 2;
        int outMid = (outHigh + outLow) / 2;

        setval = (int) ((double) (setval - inMid) * scale) + outMid;

        cmd = Integer.toString(setval); //TODO: Add lims (ip and op) check / exceptions

        return cmd;
    }
    private String getIntScaledCmd(String paramKey, int max, int min){
        String setting = params.get(paramKey);
        int setval = Integer.parseInt(setting);
        int outMid = 32768;
        double scale = 32768.0/1500.0;
        String cmd = "";

        if (setting.isEmpty())
            display.infoOut(String.format("Value cannot be empty: %s",paramKey),InterfaceToUI.P_USER);
        else if (setval > max)
            display.infoOut(String.format("Value exceeds maximum: %s (Max = %d)",paramKey,max),InterfaceToUI.P_USER);
        else if (setval < min)
            display.infoOut(String.format("Value is below minimum: %s (Min = %d)",paramKey,min),InterfaceToUI.P_USER);
        else if (setval <= max && setval >= min) {
            setval = (int) (Math.floor((double) (setval) * scale) + outMid);
            cmd = Integer.toString(setval);
        }
        return cmd;
    }
    private double getParamVal(String paramKey){
        String setting = params.get(paramKey);
        double setval = 0.0;
        if (!setting.isEmpty())
            setval = Double.parseDouble(setting);
        return setval;
    }
    public boolean updateSettings(){
        try
        {
            // todo display.setParamsFromFields();
            //temp test
            params.put("PGA Setting","4X");
            params.put("Sample Rate","5 Hz");
            params.put("Input Buffer","True");
            params.put("2 Electrode","False");
            params.put("Gain","3 kΩ (500 uA FS)");
            params.put("Experiment","Linear Sweep Voltammetry");

            params.put(Settings.CVPARAM[0],"0");
            params.put(Settings.CVPARAM[1],"0");
            params.put(Settings.CVPARAM[2],"0");
            params.put(Settings.CVPARAM[3],"0");
            params.put(Settings.CVPARAM[4],"-10");
            params.put(Settings.CVPARAM[5],"10");
            params.put(Settings.CVPARAM[6],"0");
            params.put(Settings.CVPARAM[7],"1");
            params.put(Settings.CVPARAM[8],"10");

            params.put(Settings.LSVPARAM[0],"0");
            params.put(Settings.LSVPARAM[1],"0");
            params.put(Settings.LSVPARAM[2],"0");
            params.put(Settings.LSVPARAM[3],"0");
            params.put(Settings.LSVPARAM[4],"-50");
            params.put(Settings.LSVPARAM[5],"50");
            params.put(Settings.LSVPARAM[6],"25");
            return true;
        }
        catch (Exception e)
        {
            display.dbgOut("Update settings failed. (" + e.toString() + ")");
            return false;
        }

    }

    public int getDataType(){
        return dataType;
    }

    public static double max(double... values)
    {
        double largest = Double.MIN_VALUE;
        for (double v : values) if (v > largest) largest = v;
        return largest;
    }

    public static double min(double... values)
    {
        double smallest = Double.MAX_VALUE;
        for (double v : values) if (v < smallest) smallest = v;
        return smallest;
    }

}
