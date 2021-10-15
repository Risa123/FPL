package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.parser.AExp;

public final class ExpressionInfo{
    private final AExp exp;
    private BuilderWriter writer = new BuilderWriter();
    private CompilerException lastEx;
    public ExpressionInfo(AExp exp){
        this.exp = exp;
    }
    public void setLastEx(CompilerException ex){
        lastEx = ex;
        writer = new BuilderWriter();
    }
    public AExp getExp(){
        return exp;
    }
    public BuilderWriter getWriter(){
        return writer;
    }
    public CompilerException getLastEx(){
        return lastEx;
    }
}