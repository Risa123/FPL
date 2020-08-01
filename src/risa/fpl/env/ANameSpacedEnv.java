package risa.fpl.env;

import risa.fpl.function.IFunction;

public abstract class ANameSpacedEnv extends SubEnv {
    public ANameSpacedEnv(AEnv superEnv) {
        super(superEnv);
    }
    public abstract String getNameSpace(IFunction caller);
}