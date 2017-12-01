package ca.mcmaster.waterqualitymonitorsuite;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.List;

import static org.apache.commons.math3.util.FastMath.abs;

/**
 * Created by DK on 2017-11-23.
 */
class CalcLinearRegression extends SimpleRegression {

    private SummaryStatistics stats;

    public CalcLinearRegression(boolean calcInt)
    {
        super(calcInt);
        stats = new SummaryStatistics();
    }

    public void addListData(List<Double> dataList){
        double d;
        for (int i = 0; i < dataList.size(); i++) {
            d =  dataList.get(i);
            super.addData((double)i,d);
            stats.addValue(d);
        }
    }
    @Override
    public void clear(){
        super.clear();
        stats.clear();
    }

    public double getStdDev(){
        return stats.getStandardDeviation();
    }

    public double[] getStats(List<Double> dataList){
        double[] r = new double[5];
        clear();
        addListData(dataList);
        r[0] = getSlope();
        r[1] = getIntercept();
        r[2] = getRSquare();
        r[3] = getStdDev();
        if (Double.isNaN(r[0])||Double.isNaN(r[2])||Double.isNaN(r[3]))
            r[4] = Double.NaN;
        else
            r[4] = getScore(r[0], r[2], r[3]);
        return r;
    }

    public double getScore (double slope, double r2, double stdDev){
        double score = 1-(2*abs(slope)+0.5*stdDev+r2*r2);
        //check score is within limits 0-1
        score = (score < 0) ? 0 : score;
        score = (score > 1) ? 1 : score;
        return score*100;
    }

}
