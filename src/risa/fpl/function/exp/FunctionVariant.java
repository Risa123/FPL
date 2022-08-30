package risa.fpl.function.exp;

import risa.fpl.info.TypeInfo;

public final class FunctionVariant{
    private final String cname,implName,attrCode;
    private final TypeInfo[]args;
    private int line;
    private final FunctionType type;
    public FunctionVariant(TypeInfo[]args,FunctionType type,String cname,String implName,String attrCode){
        this.cname = cname;
        this.implName = implName;
        this.args = args;
        this.attrCode = attrCode;
        this.type = type;
    }
    public String getCname(){
        return cname;
    }
    public String getImplName(){
        return implName;
    }
    public TypeInfo[]getArgs(){
        return args;
    }
    public int getLine(){
        return line;
    }
    public void setLine(int line){
        this.line = line;
    }
    public String getAttrCode(){
        return attrCode;
    }
    public FunctionType getType(){
        return type;
    }
    public boolean isVirtual(){
        return type == FunctionType.VIRTUAL || type == FunctionType.ABSTRACT;
    }
}