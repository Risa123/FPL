package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class FunctionDereference extends AField{
    private final Function func;
    public FunctionDereference(Function func){
        this.func = func;
    }
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        writePrev(builder);
        func.prepareForDereference();
        return func.compile(builder,env,it,line,tokenNum);
    }
}