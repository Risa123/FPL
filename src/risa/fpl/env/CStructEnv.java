package risa.fpl.env;

import risa.fpl.function.IFunction;
import risa.fpl.function.exp.IField;
import risa.fpl.info.TypeInfo;

public final class CStructEnv extends SubEnv{
    private final TypeInfo type;
    public CStructEnv(AEnv superEnv,String id){
        super(superEnv);
        type = new TypeInfo(id,id);
        superEnv.addType(id,type);
    }
    @Override
    public void addFunction(String name, IFunction func){
        if(func instanceof IField field){
            type.addField(name,field);
        }else{
            super.addFunction(name,func);
        }
    }
    public TypeInfo getType(){
        return type;
    }
}