package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Alias implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env, ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var id = it.nextID();
        if(env.hasFunctionInCurrentEnv(id.getValue())){
            throw new CompilerException(id,"there is already a function called " + id);
        }
        env.addFunction(id.getValue(),env.getFunction(it.nextID()));
        return TypeInfo.VOID;
    }
}