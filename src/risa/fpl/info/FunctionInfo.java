package risa.fpl.info;

import risa.fpl.function.exp.Function;

public class FunctionInfo extends TypeInfo{
    private final Function function;
    public FunctionInfo(Function function){
        super(function.getName(),function.getPointerVariant().cname(),true);
        this.function = function;
    }
    public Function getFunction(){
        return function;
    }
    @Override
    public boolean equals(Object o){
        if(o instanceof FunctionInfo f){
            return function.hasSignature(f.getFunction());
        }
        return false;
    }
}