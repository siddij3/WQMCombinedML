package ca.mcmaster.potentiostat;

/**
 * Created by DK on 2017-06-04.
 */
public class MeasData {
    double gain;
    double voltage, current;
    int ivoltage, icurrent;

    MeasData(int g){
        gain = (double)g;
        ivoltage = 0;
        icurrent = 0;
        setDoubleVals();;
    }

    MeasData(int g, int v, int c){
        gain = (double)g;
        ivoltage = v;
        icurrent = c;
        setDoubleVals();
    }
    //TODO current_A = (current+gain_trim)*(1.5/gain/8388607) gain_trim = 0     gain = 3000
    //TODO dummy_commands = ["EA2 72 2 ", "EG2 0 ", "EL0 0 32768 32768 -500 500 50 "]

    private void setDoubleVals(){
        voltage = (ivoltage-32768.0)*(gain/65536.0);
        current = (icurrent+0.0)*(1.5/gain/8388607.0);
    }

    public void setData(int v, int c){
        ivoltage = v;
        icurrent = c;
        setDoubleVals();
    }
    @Override
    public String toString() {
        String s = String.format(  "Voltage (int) = %d   Current (int) = %d    " +
                        "Voltage (mV) = %.2f   Current (A) = %.3E", ivoltage, icurrent, voltage, current);
        return s;
    }


}
