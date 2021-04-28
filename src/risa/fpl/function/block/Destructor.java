package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Destructor extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        if(!(env instanceof ClassEnv cEnv)){
            throw new CompilerException(line,charNum,"can only be declared in class block");
        }
        var b = new BuilderWriter(writer);
        b.write("void ");
        b.write(cEnv.getDestructorCall());
        b.write("{\n");
        it.nextList().compile(b,env,it);
        b.write("}\n");
        cEnv.appendFunctionCode(b.getCode());
        return TypeInfo.VOID;
    }
}