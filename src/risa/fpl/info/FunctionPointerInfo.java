package risa.fpl.info;

import risa.fpl.function.exp.Function;

public final class FunctionPointerInfo extends FunctionInfo{
    private final String cname;
    public FunctionPointerInfo(Function function){
        super(function);
        cname = getFunction().getPointerVariant().cname();
        for(var type:function.getRequiredTypes()){
            addRequiredType(type);
        }
    }
    @Override
    public boolean isPrimitive(){
        return false;
    }
    @Override
    public String getDeclaration(){
        return "typedef " + getPointerVariableDeclaration(cname) + ";\n";
    }
    @Override
    public String getCname(){
        return cname;
    }
}