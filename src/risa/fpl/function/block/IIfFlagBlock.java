package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

import java.io.BufferedWriter;
import java.io.IOException;

public class IIfFlagBlock extends ATwoPassBlock implements IFunction{
    private final List list;
    public IIfFlagBlock(List list){
        this.list = list;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        compile(writer,env,list);
        return TypeInfo.VOID;
    }
}