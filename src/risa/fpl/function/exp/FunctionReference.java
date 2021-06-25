package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.FunctionInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class FunctionReference extends ValueExp{
    private final FunctionInfo info;
    public FunctionReference(PointerInfo p){
        super(p,"&" + ((FunctionInfo)p.getType()).getFunction().getPointerVariant().cname());
        info = (FunctionInfo)p.getType();
    }
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        if(info.getFunction().notFunctionPointer()){
            throw new CompilerException(line, tokenNum,"this function has more than one variant so it can't be a function pointer");
        }
        return super.compile(writer,env,it,line, tokenNum);
    }
}