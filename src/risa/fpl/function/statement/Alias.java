package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
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
        var f = env.getFunction(it.nextID());
        if(f instanceof Function){
            f = ((Function) f).changeAccessModifier(env.getAccessModifier());
        }
        env.addFunction(id.getValue(),f);
        return TypeInfo.VOID;
    }
}