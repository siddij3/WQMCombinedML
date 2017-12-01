package ca.mcmaster.potentiostat;

/**
 * Created by DK on 2017-11-30.
 */

class Experiment {
    private String name;
    private int experimentType;
    private String cmds[] = new String[3];

    public static int EXP_CV = 0;
    public static int EXP_DPV = 2;

    Experiment(String n, int e){
        name = n;
        experimentType = e;
    }

    public String getName() {
        return name;
    }

    public void setCmds(String[] cmds) {
        this.cmds = cmds;
    }
}
