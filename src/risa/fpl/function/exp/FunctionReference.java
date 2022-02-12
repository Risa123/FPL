package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.FunctionInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class FunctionReference extends ValueExp{
    private final FunctionInfo info;
    public FunctionReference(FunctionInfo info){
        super(info,"&" + info.getFunction().getPointerVariant().getCname());
        this.info = info;
    }
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        if(info.getFunction().notFunctionPointer()){
            throw new CompilerException(line,tokenNum,"this function has more than one variant so it can't be a function pointer");
        }
        return super.compile(builder,env,it,line,tokenNum);
    }
}