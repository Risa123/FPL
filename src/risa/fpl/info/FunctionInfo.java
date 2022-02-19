package risa.fpl.info;

import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionDereference;
import risa.fpl.function.exp.PointerSize;

public class FunctionInfo extends TypeInfo{
    private final Function function;
    public FunctionInfo(Function function){
        super(function.getName(),getFunctionPointerDeclaration(function,function.getPointerVariant().getCname()));
        this.function = function;
        addField("drf",new FunctionDereference(function));
        addField("getObjectSize",PointerSize.INSTANCE);
        setClassInfo(ClassInfo.FUNCTION);
    }
    public Function getFunction(){
        return function;
    }
    @Override
    public boolean equals(Object o){
        return o instanceof FunctionInfo f?function.hasSignature(f.getFunction()):o == NIL;
    }
    private static String getFunctionPointerDeclaration(Function function,String cID){
        var b = new StringBuilder(function.getReturnType().getCname());
        b.append("(*").append(cID).append(")(");
        var self = function.getSelf();
        var firstArg = self == null;
        if(!firstArg){
            if(self instanceof InterfaceInfo){
                b.append("void");
            }else{
                b.append("struct ").append(self.getCname());
            }
            b.append("* this");
        }
        var variant = function.getPointerVariant();
        for(var arg:variant.getArgs()){
            if(firstArg){
                firstArg = false;
            }else{
                b.append(',');
            }
            b.append(arg instanceof PointerInfo p && p.getType() instanceof NonTrivialTypeInfo && self instanceof InterfaceInfo?"void*":arg.getCname());
        }
        return b.append(')').toString();
    }
    public String getPointerVariableDeclaration(String cname){
        return getFunctionPointerDeclaration(function,cname);
    }
}