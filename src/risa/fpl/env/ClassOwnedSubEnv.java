package risa.fpl.env;

import risa.fpl.info.ClassInfo;

public class ClassOwnedSubEnv extends SubEnv implements IClassOwnedEnv {
    public ClassOwnedSubEnv(AEnv superEnv) {
        super(superEnv);
    }
    @Override
    public ClassInfo getClassType() {
        if(superEnv instanceof IClassOwnedEnv e){
            return e.getClassType();
        }
        return null;
    }
}