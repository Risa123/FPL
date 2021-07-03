package risa.fpl.env;

import risa.fpl.info.ClassInfo;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;

public class FnSubEnv extends SubEnv implements IClassOwnedEnv{
    protected boolean returnNotUsed = true;
    public FnSubEnv(AEnv superEnv){
        super(superEnv);
    }
    @Override
    public final ClassInfo getClassType(){
        if(superEnv instanceof IClassOwnedEnv e){
            return e.getClassType();
        }
        return null;
    }
    public final void addTemplateInstance(InstanceInfo type){
        if(superEnv instanceof ANameSpacedEnv e){
            e.addTemplateInstance(type);
        }else{
            ((FnSubEnv)superEnv).addTemplateInstance(type);
        }
    }
    public final boolean isReturnNotUsed(){
        return returnNotUsed;
    }
    public TypeInfo getReturnType(){
        returnNotUsed = false;
        return ((FnSubEnv)superEnv).getReturnTypeInternal();
    }
    protected TypeInfo getReturnTypeInternal(){
        return ((FnSubEnv)superEnv).getReturnTypeInternal();
    }
    public boolean isInMainBlock(){
        return ((FnSubEnv)superEnv).isInMainBlock();
    }
}