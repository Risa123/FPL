package risa.fpl.info;

import risa.fpl.function.exp.Function;

public final class FunctionInfo extends TypeInfo{
    private final Function function;
    public FunctionInfo(String name,Function function){
        super(name,"",true);
        this.function = function;
    }
}