package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.FnSubEnv;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class TimesLoop extends ABlock{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        builder.append("for(");
        var iTypeAtom = it.nextAtom();
        var b = new StringBuilder();
        var iType = iTypeAtom.compile(b,env,it);
        if(iType.notIntegerNumber()){
            throw new CompilerException(iTypeAtom,"expression returning integer expected");
        }
        builder.append(iType.getCname()).append(" i=0;i<").append(b).append(";i++){\n");
        it.nextList().compile(builder,new FnSubEnv(env),it);
        builder.append("}\n");
        return TypeInfo.VOID;
    }
}