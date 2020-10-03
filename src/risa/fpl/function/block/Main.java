package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Main implements IFunction {
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        if(!(env instanceof ModuleEnv modEnv)){
            throw new CompilerException(line,charNum,"this can only be used in main module");
        }
        var fnEnv = new FnEnv(env, NumberInfo.INT,null);
        fnEnv.addFunction("argc",new Variable(NumberInfo.INT,"argc","argc"));
        fnEnv.addFunction("argv",new Variable(new PointerInfo(TypeInfo.STRING),"argv","argv"));
        writer.write(modEnv.getInitializer("_init"));
        modEnv.initCalled();
        writer.write("static int fpl_main(int argc,char** argv){\n");
        writer.write(modEnv.getInitializerCall());
        for(var e:modEnv.getModuleEnvironments()){
            if(!e.isInitCalled()){
                e.initCalled();
                writer.write("void ");
                writer.write(e.getInitializerCall());
                writer.write(e.getInitializerCall());
            }
        }
        it.nextList().compile(writer,fnEnv,it);
        if(!fnEnv.isReturnUsed()){
            writer.write("return 0;\n}\n");
        }
        writer.write("int main(int argc,char** argv){\n");
        writer.write("_Thread mainThread = static_std_lang_Thread_new(\"Main\");\n");
        writer.write("_std_lang_currentThread = &mainThread;\n");
        writer.write("_std_lang_currentThread->_throwTarget = &_std_lang_defaultExceptionHandler;\n");
        writer.write("return fpl_main(argc,argv);\n}");
        return TypeInfo.VOID;
    }
}