package risa.fpl.env;

import risa.fpl.info.ClassInfo;
import risa.fpl.info.InstanceInfo;

import java.util.ArrayList;

public class FnSubEnv extends SubEnv implements IClassOwnedEnv{
    private final ArrayList<InstanceInfo>instanceVariables = new ArrayList<>();
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
    public void addInstanceVariable(InstanceInfo instanceVariable){
        instanceVariables.add(instanceVariable);
    }
}