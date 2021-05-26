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
        if(modEnv.multipleMainDeclared()){
            throw new CompilerException(line,charNum,"declaration of multiple main blocks is not allowed");
        }
        modEnv.declareMain();
        var fnEnv = new FnEnv(env,NumberInfo.INT);
        fnEnv.addFunction("argc",new Variable(NumberInfo.INT,"argc","argc"));
        fnEnv.addFunction("args",new Variable(new PointerInfo(modEnv.getFPL().getString()),"_args","args"));
        fnEnv.addFunction("mainThread",new Variable(modEnv.getType(new Atom(0,0,"Thread",TokenType.ID)),"mainThread","mainThread"));
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
                    if(modEnv == e){
                        b.write(e.getInitializerCode());
                    }else{
                        b.write(e.getInitializerCall());
                    }
                    iterator.remove();
                }
            }
        }
        b.write("_Thread mainThread;\n");
        b.write("I_std_lang_Thread_init0(&mainThread,static_std_lang_String_new0(\"Main\",4,0));\n");
        b.write("_std_lang_currentThread = &mainThread;\n");
        b.write("_String* _args = malloc(argc * sizeof(_String));\n");
        b.write("for(int i = 0;i < argc;++i){\n");
        b.write("I_std_lang_String_init0(_args + i,argv[i],strlen(argv[i]),0);\n");
        b.write("}\n");
        it.nextList().compile(b,fnEnv,it);
        fnEnv.compileDestructorCalls(b);
        var modules1 = new ArrayList<ModuleEnv>();
        addDependencies(modEnv,modules1);
        while(!modules1.isEmpty()){
            var iterator = modules1.iterator();
            while(iterator.hasNext()){
                var e = iterator.next();
                if(e.allDependenciesDestructorCalled()){
                    if(!e.isMain()){
                        e.destructorCalled();
                        b.write(e.getDestructorCall());
                    }
                    iterator.remove();
                }
            }
        }
        b.write(modEnv.getDestructor());
        if(fnEnv.notReturnUsed()){
            b.write("free(_args);\nreturn 0;\n}");
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