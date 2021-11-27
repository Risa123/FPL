package risa.fpl.function.exp;

import risa.fpl.BuilderWriter;
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
    public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var prev = new BuilderWriter();
        prev.write("(*");
        writePrev(prev);
        prev.write(')');
        return compileChainedCall(type,writer,env,it,prev.getCode());
    }
}