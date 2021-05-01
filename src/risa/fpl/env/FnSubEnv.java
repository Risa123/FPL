package risa.fpl.env;

import risa.fpl.info.ClassInfo;
import risa.fpl.info.InstanceInfo;

public class FnSubEnv extends SubEnv implements IClassOwnedEnv{
    public FnSubEnv(AEnv superEnv){
        super(superEnv);
    }
    @Override
    public ClassInfo getClassType(){
        if(superEnv instanceof IClassOwnedEnv e){
            return e.getClassType();
        }
        return null;
    }
    public void addTemplateInstance(InstanceInfo type){
        if(superEnv instanceof ANameSpacedEnv e){
            e.addTemplateInstance(type);
        }else{
            ((FnSubEnv)superEnv).addTemplateInstance(type);
        }
    }
}