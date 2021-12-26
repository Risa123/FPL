package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Dereference extends AField{
    private final TypeInfo type;
    public Dereference(TypeInfo type){
        this.type = type;
    }
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        var prev = new StringBuilder();
        prev.append("(*");
        writePrev(prev);
        prev.append(')');
        return compileChainedCall(type,builder,env,it,prev.toString());
    }
}