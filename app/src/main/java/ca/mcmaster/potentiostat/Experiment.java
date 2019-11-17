package ca.mcmaster.potentiostat;

/**
 * Created by DK on 2017-11-30.
 */

class Experiment {
    private String name;
    private int experimentType;
    private String cmds[] = new String[3];

    public static final int EXP_CV = 0;
    public static final int EXP_DPV = 2;

    Experiment(String n, int e){
        name = n;
        experimentType = e;
    }

    public String getName() {
        return name;
    }

    public String getTypeString(){
        switch (experimentType){
            case EXP_CV:
                return "CV";

            case EXP_DPV:
                return "DPV";

            default:
                return null;

        }
    }

    public void setCmds(String[] cmds) {
        this.cmds = cmds;
    }
}
