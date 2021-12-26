package risa.fpl.info;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Cast;
import risa.fpl.function.exp.UnaryOperator;
import risa.fpl.function.exp.ValueExp;

public final class InterfaceInfo extends NonTrivialTypeInfo{
    private final String implName;
    public InterfaceInfo(ModuleEnv module,String name){
        super(module,name,IFunction.toCId(name));
        addField("cast",new Cast(this));
        implName = "I" + getCname() + "_impl";
        addField("getObjectSize",new UnaryOperator(NumberInfo.MEMORY,"sizeof ",false));
        getClassInfo().addField("getInstanceSize",new ValueExp(NumberInfo.MEMORY,"sizeof(" + getCname() + ')'));
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