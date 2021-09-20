package risa.fpl.env;

import risa.fpl.info.TypeInfo;

import java.util.ArrayList;

public final class ConstructorEnv extends FnEnv{
    private final ArrayList<String>definedConstFields = new ArrayList<>();
    public ConstructorEnv(AEnv superEnv){
        super(superEnv,TypeInfo.VOID);
    }
    public ArrayList<String>getDefinedConstFields(){
        return definedConstFields;
    }
}