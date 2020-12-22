package risa.fpl.env;

import risa.fpl.info.ClassInfo;

public class FnSubEnv extends SubEnv implements IClassOwnedEnv{
    public FnSubEnv(AEnv superEnv) {
        super(superEnv);
    }
    @Override
    public ClassInfo getClassType(){
        if(superEnv instanceof IClassOwnedEnv e){
            return e.getClassType();
        }
        return null;
    }
}