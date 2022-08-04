package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AtomType;
import risa.fpl.parser.ExpIterator;

public final class TertiaryOperator extends AField{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        writePrev(builder);
        builder.append('?');
        var ifTrueReturn = it.next().compile(builder,env,it);
        builder.append(':');
        var separator = it.nextAtom();
        if(separator.getType() != AtomType.ARG_SEPARATOR){
            error(separator,", expected");
        }
        var ifFalseExp = it.next();
        if(!ifFalseExp.compile(builder,env,it).equals(ifTrueReturn)){
            error(ifFalseExp,"expression expected to return " + ifTrueReturn);
        }
        return ifTrueReturn;
    }
}