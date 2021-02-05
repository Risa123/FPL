package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Main implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        if(!(env instanceof ModuleEnv modEnv)){
            throw new CompilerException(line,charNum,"this can only be used in main module");
        }
        var fnEnv = new FnEnv(env,NumberInfo.INT);
        fnEnv.addFunction("argc",new Variable(NumberInfo.INT,"argc","argc"));
        fnEnv.addFunction("argv",new Variable(new PointerInfo(TypeInfo.STRING),"argv","argv"));
        fnEnv.addFunction("mainThread",new Variable(modEnv.getType(new Atom(0,0,"Thread", TokenType.ID)),"mainThread","mainThread"));
        writer.write(modEnv.getInitializer("_init"));
        modEnv.initCalled();
        var b = new BuilderWriter(writer);
        b.write("int main(int argc,char** argv){\nvoid ");
        b.write(modEnv.getInitializerCall());
        for(var e:modEnv.getModuleEnvironments()){
            if(!e.isInitCalled()){
                e.initCalled();
                b.write("void ");
                b.write(e.getInitializerCall()); //declaration
                b.write(e.getInitializerCall()); //call
            }
        }
        b.write("_Thread mainThread;\n");
        b.write("I_std_lang_Thread_init(&mainThread,\"Main\");\n");
        b.write("_std_lang_currentThread = &mainThread;\n");
        it.nextList().compile(b,fnEnv,it);
        if(fnEnv.notReturnUsed()){
            b.write("return 0;\n}\n");
        }
        modEnv.appendFunctionCode(b.getCode());
        return TypeInfo.VOID;
    }
}