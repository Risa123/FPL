package risa.fpl.env;

import risa.fpl.info.InstanceInfo;

public final class ConditionalBlockEnv extends FnSubEnv{
    private boolean compilingCondition = true;
    public ConditionalBlockEnv(AEnv superEnv){
        super(superEnv);
    }
    public void turnOffCompilingCondition(){
        compilingCondition = false;
    }
    @Override
    protected String addToPointerVar(InstanceInfo type){
        if(compilingCondition){
            ((SubEnv)superEnv).addToPointerVar(type);
        }
        return super.addToPointerVar(type);
    }
}