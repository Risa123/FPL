package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Alias implements IFunction{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env, ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum);
        var id = it.nextID();
        if(env.hasFunctionInCurrentEnv(id.getValue())){
            throw new CompilerException(id,"there is already a function called " + id);
        }
        var f = env.getFunction(it.nextID());
        if(f instanceof Function fn){
            f = fn.changeAccessModifier(env.getAccessModifier());
        }
        env.addFunction(id.getValue(),f);
        if(it.hasNext()){
            throw new CompilerException(line,tokenNum,"no tokens allowed after alias");
        }
        return TypeInfo.VOID;
    }
}