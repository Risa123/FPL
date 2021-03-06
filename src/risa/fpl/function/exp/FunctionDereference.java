package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public  final class FunctionDereference extends AField{
    private final Function func;
    public FunctionDereference(Function func){
        this.func = func;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        writePrev(writer);
        return func.compile(writer,env,it,line,charNum);
    }
}