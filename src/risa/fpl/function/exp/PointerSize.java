package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class PointerSize extends AField{
    public static final PointerSize INSTANCE = new PointerSize();
    private PointerSize(){}
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        builder.append(NumberInfo.MEMORY.getSize());
        var prevCode = getPrevCode();
        if(prevCode == null){
            prevCode = "";
        }
        return compileChainedCall(NumberInfo.MEMORY,builder,env,it,prevCode);
    }
}