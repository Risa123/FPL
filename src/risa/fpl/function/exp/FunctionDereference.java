package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class FunctionDereference extends AField{
    private final Function func;
    public FunctionDereference(Function func){
        this.func = func;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        writePrev(writer);
        func.prepareForDereference();
        return func.compile(writer,env,it,line,tokenNum);
    }
}