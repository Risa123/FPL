package risa.fpl.function;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.block.IIfFlagBlock;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

import java.io.BufferedWriter;
import java.io.IOException;

public final class IfFlag implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env, ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var flag = it.nextID().getValue();
        var exp = it.next();
        if(env.getFPL().hasFlag(flag)){
           if(exp instanceof List list){
               new IIfFlagBlock(list).compile(writer,env,it,line,charNum);
           }else{
               exp.compile(writer,env,it);
           }
        }
        return TypeInfo.VOID;
    }
}