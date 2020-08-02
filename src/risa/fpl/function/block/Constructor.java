package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Constructor extends AFunctionBlock {
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        var cEnv = (ClassEnv)env;
        var type = cEnv.getInstanceType();
        var b = new BuilderWriter(writer);
        b.write("void I");
        b.write(cEnv.getNameSpace(this));
        b.write("_init");
        var fEnv = new FnEnv(env,TypeInfo.VOID,type.getClassInfo());
        var constructor = new ClassVariable(cEnv.getInstanceType(),cEnv.getClassType(), parseArguments(b,it,fEnv,type),cEnv.getNameSpace(this));
        type.setConstructor(constructor);
        b.write("{\n");
        it.nextList().compile(b,fEnv,it);
        b.write("}\n");
        cEnv.addMethod(constructor,b.getText());
        return TypeInfo.VOID;
    }
}