package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.FunctionInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class FunctionReference extends ValueExp{
    private final FunctionInfo info;
    public FunctionReference(FunctionInfo info){
        super(info,"&" + info.getFunction().getPointerVariant().cname());
        this.info = info;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        if(info.getFunction().notFunctionPointer()){
            throw new CompilerException(line,tokenNum,"this function has more than one variant so it can't be a function pointer");
        }
        return super.compile(writer,env,it,line,tokenNum);
    }
}