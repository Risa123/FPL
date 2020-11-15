package risa.fpl.env;

import risa.fpl.info.ClassInfo;

public class ClassOwnedSubEnv extends SubEnv implements IClassOwnedEnv {
    public ClassOwnedSubEnv(AEnv superEnv) {
        super(superEnv);
    }
    @Override
    public ClassInfo getClassType() {
        return ((IClassOwnedEnv)superEnv).getClassType();
    }
}