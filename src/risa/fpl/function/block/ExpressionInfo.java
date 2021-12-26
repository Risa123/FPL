package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.parser.AExp;

public final class ExpressionInfo{
    private final AExp exp;
    private final StringBuilder builder = new StringBuilder();
    private CompilerException lastEx;
    public ExpressionInfo(AExp exp){
        this.exp = exp;
    }
    public void setLastEx(CompilerException ex){
        lastEx = ex;
        builder.setLength(0);
    }
    public AExp getExp(){
        return exp;
    }
    public StringBuilder getBuilder(){
        return builder;
    }
    public CompilerException getLastEx(){
        return lastEx;
    }
}