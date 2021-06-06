package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class CopyConstructor extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        if(!(env instanceof ClassEnv cEnv)){
            throw new CompilerException(line,charNum,"can only be declared in class block");
        }
        if(cEnv.isCopyConstructorDeclared()){
            throw new CompilerException(line,charNum,"copy constructor already declared");
        }
        var b = new BuilderWriter(writer);
        b.write("void ");
        var type = cEnv.getInstanceType();
        var copyName = INTERNAL_METHOD_PREFIX + cEnv.getNameSpace() + "_copy";
        type.setCopyConstructorName(copyName);
        b.write(copyName);
        b.write("(" + type.getCname() + "* this," + type.getCname() + "* o){\n");
        var fnEnv = new FnEnv(env,TypeInfo.VOID);
        fnEnv.addFunction("o",new Variable(new PointerInfo(type),"o","o"));
        it.nextList().compile(b,fnEnv,it);
        b.write("}\n");
        cEnv.appendFunctionCode(b.getCode());
        cEnv.declareCopyConstructor();
        return TypeInfo.VOID;
    }
}