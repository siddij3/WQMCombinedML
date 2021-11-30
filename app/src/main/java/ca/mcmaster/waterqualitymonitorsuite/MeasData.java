package ca.mcmaster.waterqualitymonitorsuite;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by DK on 2017-10-28.
 */
/* Class for holding raw data for each measurement and calculating values for
    temperature, pH, and free chlorine.*/

class MeasData {
    private static final String TAG = MeasData.class.getSimpleName();

    final static int CALC_TEMPERATURE = 0;
    final static int CALC_PH = 1;
    final static int CALC_CL = 2;
    final static int CALC_ALK = 3;
    final static int RAW_TEMPERATURE = 4;
    final static int RAW_VOLTAGE = 5;
    final static int RAW_CURRENT = 6;
    final static int RAW_ALK = 7;
    final static int TIME_STAMP = 8;
    final static int CL_SW = 9;
    final static int SW_TIME = 10;


    private final static double BASE_SENS_PH = 60.0; //60.6
    private final static double BASE_SENS_CL = 342.0;
    private final static double BASE_LVL_CL = 0.0;
    private final static double BASE_OFFSET_CL = 109.6;

    private final static double BASE_SENS_ALK = 342.0;
    private final static double BASE_LVL_ALK = 0.0;
    private final static double BASE_OFFSET_ALK = 109.6;



    private double phCal7, phSensLo, phSensHi; // Calibration values used for pH calculation
    private double Cl_Cal_i, Cl_Cal_lvl, Cl_Sens; // Calibration values used for free Cl calculation
    private double Alk_Cal_i, Alk_Cal_lvl, Alk_Sens; // Calibration values used for free Cl calculation

    private double tCal, tSens; //Calibration values for T calculation
    private double temperature, phValue, chlorineValue, alkalinityValue; //Calculated temperature, pH, and free Cl
    private double rawT, rawE, rawI; //Raw values for temperature, voltage (pH), current (Cl)

    private double rawA; //Raw value for alkalinity

    private double measTime; // free Cl measurement time
    private boolean swOn; // free Cl measurement switch on
    private String timeStamp; //time stamp for recorded measurement

    private boolean avgOk;
    private double pH_stats[];
    private double t_stats[];

    MeasData(double rawT, double rawE, double rawI, double rawA, double phCal7, double phSensLo, double phSensHi, double tCal, double tSens){
        this.rawT = rawT;
        this.rawE = rawE;
        this.rawI = rawI;
        this.rawA = rawA;

        this.phCal7 = phCal7;
        this.phSensLo = phSensLo;
        this.phSensHi = phSensHi;
        this.tCal = tCal;
        this.tSens = tSens;

        Cl_Cal_lvl = BASE_LVL_CL;
        Cl_Cal_i = BASE_OFFSET_CL;
        Cl_Sens = BASE_SENS_CL;

        Alk_Cal_lvl = BASE_LVL_ALK;
        Alk_Cal_i = BASE_OFFSET_ALK;
        Alk_Sens = BASE_SENS_ALK;


        measTime = 0;
        swOn = false;

        temperature = calcT(rawT);
        phValue = calcPh(rawE, temperature);
        chlorineValue = calcCl(rawI, phValue, temperature);
        alkalinityValue = calcAlk(rawI, phValue, temperature);

        timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.CANADA).format(new Date());

        //Calibration stats
        avgOk = false;
        pH_stats = new double[5];
        t_stats = new double[5];
    }
    //Meas data calculation currently used
    MeasData(double rawT, double rawE, double rawI, double rawA, double measTime, boolean swOn, double phCal7, double phSensLo, double phSensHi, double tCal, double tSens, double Cl_Cal_i, double Cl_Cal_lvl, double Cl_Sens, double Alk_Cal_i, double Alk_Cal_lvl, double Alk_Sens){
        this.rawT = rawT;
        this.rawE = rawE;
        this.rawI = rawI;
        this.rawA = rawA;

        this.phCal7 = phCal7;
        this.phSensLo = phSensLo;
        this.phSensHi = phSensHi;
        this.tCal = tCal;
        this.tSens = tSens;

        this.Cl_Cal_i = Cl_Cal_i;
        this.Cl_Cal_lvl = Cl_Cal_lvl;
        this.Cl_Sens = Cl_Sens;

        this.Alk_Cal_i = Alk_Cal_i;
        this.Alk_Cal_lvl = Alk_Cal_lvl;
        this.Alk_Sens = Alk_Sens;

        this.measTime = measTime;
        this.swOn = swOn;

        temperature = calcT(rawT);
        phValue = calcPh(rawE); //phValue = calcPh(rawE, temperature);
        chlorineValue = calcCl(rawI); //chlorineValue = calcCl(rawI, phValue, temperature);
        alkalinityValue = calcAlk(rawA); //chlorineValue = calcCl(rawI, phValue, temperature);

        timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.CANADA).format(new Date());

        //Calibration stats
        avgOk = false;
        pH_stats = new double[5];
        t_stats = new double[5];
    }

    public void setpH_stats(double[] stats){
        if (stats.length==pH_stats.length) {
            pH_stats = stats.clone();
            avgOk = true;
        }
    }

    public double[] getpH_stats(){
        return pH_stats;
    }

    public void setT_stats(double[] stats){
        if (stats.length == t_stats.length) {
            t_stats = stats.clone();
            avgOk = true;
        }
    }

    public double[] getT_stats(){
        return t_stats;
    }


    public boolean getAvgOk(){
        return avgOk;
    }

    //Temp. calculation from supplied potential
    private double calcT(double e){
        double result = (e-tCal)/tSens;
        return result;
    }


    //pH calculation from supplied potential and temperature
    //broken up to simplify calculation, f_* = function of *
    private double calcPh(double e, double t){
        Log.d(TAG, String.format("calcCl: e: %.3f t: %.3f",e,t));
        double sens;
        //determine sensitivity to use based on voltage
        if(phSensLo < 0 && phSensHi < 0) {
            //if voltage is greater than Vph7 (ie pH < 7) use phSensLo else use phSensHi
            sens = (e > phCal7) ? phSensLo : phSensHi;
        } else if (phSensLo > 0 && phSensHi > 0) {
            //if voltage is less than Vph7 (ie pH < 7) use phSensLo else use phSensHi
            sens = (e < phCal7) ? phSensLo : phSensHi;
        } else {
            //sensitivity invalid, use base
            sens = BASE_SENS_PH;
        }
        double f_e = phCal7 - e;
        double sf_t = -sens + (t - 27)*0.23; //Temperature dependant sensitivity
        double result = f_e/sf_t + 7;
        if (result > 14)
            result = 14.0;
        else if (result < 0)
            result = 0.0;
        Log.d(TAG, String.format("calcPh: f_e: %.3f f_t: %.3f pH: %.2f",f_e,sf_t,result));
        return result;
    }

    //simplified pH calculation from supplied potential only
    //broken up to simplify calculation, f_* = function of *
    private double calcPh(double e){
        Log.d(TAG, String.format("calcCl: e: %.3f",e));
        double sens;
        //determine sensitivity to use based on voltage
        if(phSensLo < 0 && phSensHi < 0) {
            //if voltage is greater than Vph7 (ie pH < 7) use phSensLo else use phSensHi
            sens = (e > phCal7) ? phSensLo : phSensHi;
        } else if (phSensLo > 0 && phSensHi > 0) {
            //if voltage is less than Vph7 (ie pH < 7) use phSensLo else use phSensHi
            sens = (e < phCal7) ? phSensLo : phSensHi;
        } else {
            //sensitivity invalid, use base
            sens = BASE_SENS_PH;
        }
        double f_e = phCal7 - e;
        double result = f_e/(-sens) + 7;
        if (result > 14)
            result = 14.0;
        else if (result < 0)
            result = 0.0;
        Log.d(TAG, String.format("calcPh: f_e: %.3f pH: %.2f",f_e,result));
        return result;
    }

    /*Cl calculation from supplied, current, pH and temperature
      broken up to simplify calculation, f_* = function of *

      C = k*(f_i/sf_t)*f_ph_t
      where f_ph_t = 1+10^(ph - f_t) */

    private double calcCl(double i, double ph, double t){
        double k, f_i, sf_t, f_ph_t, f_t, t2, result;
        Log.d(TAG, String.format("calcCl: i: %.3f ph: %.3f t: %.2f",i,ph,t));

        k = 0.57;
        f_i = i - Cl_Cal_i;
        sf_t = Cl_Sens + (t-27) * 9.3;
        t2 = t + 273;
        f_t = (3000/t2)-10.0686+(0.0253*t2);
        f_ph_t = 1 + Math.pow(10,ph - f_t);

        result = k*(f_i/sf_t)*f_ph_t + Cl_Cal_lvl;

        //Set to zero if result is negative (ppm can not be negative)
        if (result < 0)
            result = 0.0;

        Log.d(TAG, String.format("calcCl: k: %.3f f_i: %.3f sf_t: %.3f f_t: %.3f f_ph_t: %.3f Cl: %.3f",k,f_i,sf_t,f_t,f_ph_t,result));
        return result;
    }

    private double calcAlk(double a, double ph, double t){
        double k, f_a, sf_t, f_ph_t, f_t, t2, result;
        Log.d(TAG, String.format("calcCl: i: %.3f ph: %.3f t: %.2f",a,ph,t));

        k = 0.57;
        f_a = a - Cl_Cal_i;
        sf_t = Cl_Sens + (t-27) * 9.3;
        t2 = t + 273;
        f_t = (3000/t2)-10.0686+(0.0253*t2);
        f_ph_t = 1 + Math.pow(10,ph - f_t);

        result = k*(f_a/sf_t)*f_ph_t + Cl_Cal_lvl;

        //Set to zero if result is negative (ppm can not be negative)
        if (result < 0)
            result = 0.0;

        Log.d(TAG, String.format("calcCl: k: %.3f f_a: %.3f sf_t: %.3f f_t: %.3f f_ph_t: %.3f Cl: %.3f",k,f_a,sf_t,f_t,f_ph_t,result));
        return result;
    }

    //simplified free Cl calculation, does not consider pH level or temperate
    private double calcCl(double i){
        double k, f_i, result;
        Log.d(TAG, String.format("calcCl: i: %.3f ",i));
        //Cl_Cal_i = current offset, f_i = meas. current - Cl_Cal_i
        //Cl_Cal_lvl = level (in ppm) when f_i is 0
        //Cl_Sens = current / ppm slope

        k = 1.0;
        f_i = i - Cl_Cal_i;

        result = k*(f_i/Cl_Sens) + Cl_Cal_lvl;

        //Set to zero if result is negative (ppm can not be negative)
        if (result < 0)
            result = 0.0;

        Log.d(TAG, String.format("calcCl: k: %.3f f_i: %.3f Cl: %.3f",k,f_i,result));
        return result;
    }

    private double calcAlk(double a) {
        double k, f_a, result;
        Log.d(TAG, String.format("calcAlk: a: %.3f ", a));
        //Cl_Cal_i = current offset, f_i = meas. current - Cl_Cal_i
        //Cl_Cal_lvl = level (in ppm) when f_i is 0
        //Cl_Sens = current / ppm slope

        k = 1.0;
        f_a = a - Alk_Cal_i;

        result = k*(f_a/Alk_Sens) + Alk_Cal_lvl;

        //Set to zero if result is negative (ppm can not be negative)
        if (result < 0)
            result = 0.0;

        Log.d(TAG, String.format("calcAlk: k: %.3f f_a: %.3f Alk: %.3f",k,f_a,result));
        return result;
    }

    public Object getValue(int index){
        switch (index){
            case CALC_TEMPERATURE:
                return temperature;
            case CALC_PH:
                return phValue;
            case CALC_CL:
                return chlorineValue;
            case CALC_ALK:
                return alkalinityValue;
            case RAW_ALK:
                return rawA;
            case RAW_TEMPERATURE:
                return rawT;
            case RAW_VOLTAGE:
                return rawE;
            case RAW_CURRENT:
                return rawI;
            case TIME_STAMP:
                return timeStamp;
            case CL_SW:
                return swOn;
            case SW_TIME:
                return measTime;
            default:
                return null;
        }
    }
    @Override
    public String toString() {
        return String.format(Locale.CANADA,"Time: %s, Temp.(C): %.2f, pH Value: %.2f, free Cl (ppm): %.2f, Alkalinity (ppm): %.2f", timeStamp, temperature, phValue, chlorineValue, alkalinityValue);
    }
}
