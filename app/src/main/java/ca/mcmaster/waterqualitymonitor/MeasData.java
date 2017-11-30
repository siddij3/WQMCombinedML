package ca.mcmaster.waterqualitymonitor;

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
    final static int RAW_TEMPERATURE = 3;
    final static int RAW_VOLTAGE = 4;
    final static int RAW_CURRENT = 5;
    final static int TIME_STAMP = 6;

    private final static double BASE_SENS_PH = 60.0; //60.6
    private final static double BASE_SENS_CL = 342.0;

    private double eCal; // Calibration voltage used for pH calculation

    private double temperature, phValue, chlorineValue; //Calculated temperature, pH, and free Cl
    private double rawT, rawE, rawI; //Raw values for temperature, voltage (pH), current (Cl)
    private String timeStamp; //time stamp for recorded measurement

    private boolean avgOk;
    private double pH_stats[];
    MeasData(double rawT, double rawE, double rawI, double eCal){
        this.rawT = rawT;
        this.rawE = rawE;
        this.rawI = rawI;
        this.eCal = eCal;

        temperature = rawT;
        phValue = calcPh(rawE, temperature);
        chlorineValue = calcCl(rawI, phValue, temperature);

        timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.CANADA).format(new Date());

        //pH Calibration stats
        avgOk = false;
        pH_stats = new double[5];
    }

    public void setpHstats(double[] stats){
        if (stats.length==pH_stats.length) {
            pH_stats = stats.clone();
            avgOk = true;
        }
    }

    public double[] getpH_stats(){
        return pH_stats;
    }

    public boolean getAvgOk(){
        return avgOk;
    }

    //pH calculation from supplied potential and temperature
    //broken up to simplify calculation, f_* = function of *
    private double calcPh(double e, double t){
        Log.d(TAG, String.format("calcCl: e: %.3f t: %.3f",e,t));

        double f_e = eCal - e;
        double sf_t = BASE_SENS_PH + (t - 27)*0.23; //Temperature dependant sensitivity
        double result = f_e/sf_t + 7;
        if (result > 14)
            result = 14.0;
        else if (result < 0)
            result = 0.0;
        Log.d(TAG, String.format("calcPh: f_e: %.3f f_t: %.3f pH: %.2f",f_e,sf_t,result));
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
        f_i = i - 109.6;
        sf_t = BASE_SENS_CL + (t-27) * 9.3;
        t2 = t + 273;
        f_t = (3000/t2)-10.0686+(0.0253*t2);
        f_ph_t = 1 + Math.pow(10,ph - f_t);

        result = k*(f_i/sf_t)*f_ph_t;

        //Set to zero if result is negative (ppm can not be negative)
        if (result < 0)
            result = 0.0;

        Log.d(TAG, String.format("calcCl: k: %.3f f_i: %.3f sf_t: %.3f f_t: %.3f f_ph_t: %.3f Cl: %.3f",k,f_i,sf_t,f_t,f_ph_t,result));
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
            case RAW_TEMPERATURE:
                return rawT;
            case RAW_VOLTAGE:
                return rawE;
            case RAW_CURRENT:
                return rawI;
            case TIME_STAMP:
                return timeStamp;
            default:
                return null;
        }
    }
    @Override
    public String toString() {
        return String.format(Locale.CANADA,"Time: %s, Temp.(C): %.2f, pH Value: %.2f, free Cl (ppm): %.2f", timeStamp, temperature, phValue, chlorineValue);
    }
}
