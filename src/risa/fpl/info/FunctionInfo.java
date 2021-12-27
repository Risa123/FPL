package risa.fpl.info;

import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionDereference;
import risa.fpl.function.exp.PointerSize;

public class FunctionInfo extends TypeInfo{
    private final Function function;
    public FunctionInfo(Function function){
        super(function.getName(),getFunctionPointerDeclaration(function,function.getPointerVariant().cname()));
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
        if(o instanceof FunctionInfo f){
            return function.hasSignature(f.getFunction());
        }
        return o == NIL;
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
        for(var arg:variant.args()){
            if(firstArg){
                firstArg = false;
            }else{
                b.append(',');
            }
            if(arg instanceof PointerInfo p && p.getType() instanceof NonTrivialTypeInfo && self instanceof InterfaceInfo){
                b.append("void*");
            }else{
                b.append(arg.getCname());
            }
        }
        return b.append(")").toString();
    }
    public String getPointerVariableDeclaration(String cname){
        return getFunctionPointerDeclaration(function,cname);
    }
}