package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class PointerSize extends AField{
    public static final PointerSize INSTANCE = new PointerSize();
    private PointerSize(){}
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        writer.write(Integer.toString(NumberInfo.MEMORY.getSize()));
        return NumberInfo.MEMORY;
    }
}