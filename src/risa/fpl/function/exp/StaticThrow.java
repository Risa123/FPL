package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.FnSubEnv;
import risa.fpl.env.SubEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class StaticThrow extends Function{
    public StaticThrow(){
        super("throw",TypeInfo.VOID,AccessModifier.PUBLIC);
    }
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        if(env instanceof FnSubEnv e){
            e.getReturnType();//to prevent no return error
        }
        return super.compile(builder,env,it,line,tokenNum);
    }
}