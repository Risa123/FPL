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
import java.util.ArrayList;

public final class Main implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        if(!(env instanceof ModuleEnv modEnv && modEnv.isMain())){
            throw new CompilerException(line,charNum,"this can only be used in main module");
        }
        if(modEnv.isMainDeclared()){
            throw new CompilerException(line,charNum,"declaration of multiple main blocks is not allowed");
        }
        modEnv.declareMain();
        var fnEnv = new FnEnv(env,NumberInfo.INT);
        fnEnv.addFunction("argc",new Variable(NumberInfo.INT,"argc","argc"));
        fnEnv.addFunction("argv",new Variable(new PointerInfo(TypeInfo.STRING),"argv","argv"));
        fnEnv.addFunction("mainThread",new Variable(modEnv.getType(new Atom(0,0,"Thread", TokenType.ID)),"mainThread","mainThread"));
        writer.write(modEnv.getInitializer("_init"));
        var b = new BuilderWriter(writer);
        b.write("int main(int argc,char** argv){\n");
        var modules = new ArrayList<ModuleEnv>();
        addDependencies(modEnv,modules);
        while(!modules.isEmpty()){
            var iterator = modules.iterator();
            while(iterator.hasNext()){
                var e = iterator.next();
                if(e.allDependenciesInitCalled()){
                    e.initCalled();
                    b.write(e.getInitializerCall()); //call
                    iterator.remove();
                }
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
    private void addDependencies(ModuleEnv env,ArrayList<ModuleEnv>modules){
       if(!modules.contains(env)){
           modules.add(env);
           for(var mod:env.getImportedModules()){
               addDependencies(mod,modules);
           }
       }
    }
}