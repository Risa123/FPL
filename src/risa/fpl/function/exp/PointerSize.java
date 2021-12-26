package risa.fpl.function.exp;

import risa.fpl.env.SubEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class PointerSize extends AField{
    public static final PointerSize INSTANCE = new PointerSize();
    private PointerSize(){}
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum){
        builder.append(NumberInfo.MEMORY.getSize());
        return NumberInfo.MEMORY;
    }
}