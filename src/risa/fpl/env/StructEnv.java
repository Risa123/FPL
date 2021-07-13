package risa.fpl.env;

import risa.fpl.function.IFunction;
import risa.fpl.function.exp.IField;
import risa.fpl.info.TypeInfo;

public final class StructEnv extends SubEnv{
    private final TypeInfo type;
    public StructEnv(AEnv superEnv,String id){
        super(superEnv);
        type = new TypeInfo(id,IFunction.toCId(id));
        superEnv.addType(id,type);
    }
    @Override
    public void addFunction(String name,IFunction func){
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