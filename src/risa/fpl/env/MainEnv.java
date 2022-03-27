package risa.fpl.env;

import risa.fpl.info.NumberInfo;

public final class MainEnv extends FnEnv{
    public MainEnv(AEnv superEnv){
        super(superEnv,NumberInfo.INT);
    }
    @Override
    public boolean isInMainBlock(){
        return true;
    }
}