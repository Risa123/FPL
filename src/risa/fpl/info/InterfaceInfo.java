package risa.fpl.info;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Cast;

public final class InterfaceInfo extends NonTrivialTypeInfo{
    private final String implName;
    public InterfaceInfo(ModuleEnv module,String name){
        super(module,name,IFunction.toCId(name));
        addField("cast",new Cast(this));
        implName = "I" + getCname() + "_impl";
    }
    public String getImplName(){
        return implName;
    }
    @Override
    public boolean equals(Object o){
        if(o instanceof TypeInfo type){
            if(type instanceof PointerInfo p){
                type = p.getType();
            }
            if(type.getParents().contains(this)){
                return true;
            }
        }
        return this == o;
    }
}