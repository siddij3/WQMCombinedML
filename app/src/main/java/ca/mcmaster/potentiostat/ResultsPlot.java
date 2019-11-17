package ca.mcmaster.potentiostat;

//import java.awt.*;


public class ResultsPlot {


/*    private List<XYSeries> dataSeriesList = new ArrayList<>();
    public JFreeChart chart;
    public ChartPanel cp;

    private XYSeries defaultSeries = new XYSeries("empty");

    public XYSeriesCollection dataset;*/

    public ResultsPlot() {

    }

    public void createChart() {
        // TODO: 2017-10-20 update with android plot
      /*  dataset = new XYSeriesCollection(defaultSeries);//changed from default

        chart = ChartFactory.createXYLineChart(" ", "Voltage (mV)", "Current (A)", dataset);
        cp = new ChartPanel(chart);

        //create custom shape
        final Shape[] shape = new Shape[1];
        shape[0] = new Ellipse2D.Double(-0.5, -0.5, 1.0, 1.0);
        //create drawing supplier with custom shape
        final DrawingSupplier supplier = new DefaultDrawingSupplier(
                DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                shape
        );

        //Set the drawing supplier
        final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(supplier);

        //modify renderer, enable shapes, disable lines
        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseShapesVisible(true);
        renderer.setBaseLinesVisible(false);



        // customise the range axis...
       *//* final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setUpperMargin(0.12);*//*

        clearSeries();*/

    }

    public void clearSeries() {


    }

    public int getSeriesCount() {
        //return dataset.getSeriesCount(); //todo
        return 0;
    }


    //todo add more checks for correct series #
    public void addPoint(int series, double x, double y) {


    }
/*  todo update for android?
    public void setDomainAxis(Range range, boolean checkCurrent){

    }*/

    public void setDomainAxis(double min, double max, boolean checkCurrent) {

    }

/*
    public boolean currDatasetToArray(XYSeriesCollection ds){

    }
*/
}
