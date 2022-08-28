package risa.fpl.info;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Cast;
import risa.fpl.function.exp.UnaryOperator;
import risa.fpl.function.exp.ValueExp;

public final class InterfaceInfo extends NonTrivialTypeInfo {
    private final String implName,copyName,destructorName;
    public InterfaceInfo(ModuleEnv module,String name){
        super(module,name,IFunction.toCId(name));
        implName = IFunction.INTERNAL_PREFIX + getCname() + "_impl";
        copyName = IFunction.INTERNAL_PREFIX + module.getNameSpace() + getCname() + "_copy";
        destructorName = IFunction.INTERNAL_PREFIX + module.getNameSpace() + getCname() + "_destructor";
        addField("cast",new Cast(this));
        addField("getObjectSize",new UnaryOperator(NumberInfo.MEMORY,"sizeof ",false));
        getClassInfo().addField("getInstanceSize",new ValueExp(NumberInfo.MEMORY,"sizeof(" + getCname() + ')'));
    }
    public String getImplName(){
        return implName;
    }
    public String getCopyName(){
        return copyName;
    }
    public String getDestructorName(){
        return destructorName;
    }
    @Override
    public boolean equals(Object o) {
        if(o instanceof TypeInfo type && ((type instanceof PointerInfo p)?p.getType():type).getParents().contains(this)){
            return true;
        }
        return this == o;
    }
}