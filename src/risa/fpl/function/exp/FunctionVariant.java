package risa.fpl.function.exp;

import risa.fpl.info.TypeInfo;

public final class FunctionVariant{
    private final String cname,implName;
    private final TypeInfo[]args;
    private int line;
    public FunctionVariant(TypeInfo[]args,String cname,String implName){
        this.cname = cname;
        this.implName = implName;
        this.args = args;
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
}