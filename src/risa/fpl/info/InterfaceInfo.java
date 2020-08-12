package risa.fpl.info;

import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Cast;

public final class InterfaceInfo extends TypeInfo {
    private final String implName;
    public InterfaceInfo(String name) {
        super(name, IFunction.toCId(name),false);
        addField("cast",new Cast(this));
        implName = "I" + getCname() + "_impl";
    }
    public String getImplName(){
        return implName;
    }
}