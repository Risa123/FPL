package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.function.IFunction;
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
        if(cEnv.isDestructorDeclared()){
            throw new CompilerException(line,charNum,"destructor already declared");
        }
        cEnv.destructorDeclared();
        var b = new BuilderWriter(writer);
        b.write("void ");
        var type = cEnv.getInstanceType();
        type.setDestructorName(IFunction.INTERNAL_METHOD_PREFIX + cEnv.getNameSpace());
        b.write(type.getDestructorName() + "(" + type.getCname() + "* this){\n");
        b.write(cEnv.getImplicitDestructorCode());
        it.nextList().compile(b,new FnEnv(env,TypeInfo.VOID),it);
        b.write("}\n");
        cEnv.appendFunctionCode(b.getCode());
        return TypeInfo.VOID;
    }
}