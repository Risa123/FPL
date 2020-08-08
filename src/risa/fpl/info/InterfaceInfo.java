package risa.fpl.info;

import risa.fpl.function.IFunction;

public final class InterfaceInfo extends TypeInfo {
    private final String implName;
    public InterfaceInfo(String name) {
        super(name, IFunction.toCId(name));
        implName = "I" + getCname() + "_impl";
    }
    public String getImplName(){
        return implName;
    }
}