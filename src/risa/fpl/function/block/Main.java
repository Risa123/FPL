package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Main implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        if(!(env instanceof ModuleEnv modEnv && modEnv.isMain())){
            throw new CompilerException(line,tokenNum,"this can only be used in main module");
        }
        if(modEnv.multipleMainDeclared()){
            throw new CompilerException(line,tokenNum,"declaration of multiple main blocks is not allowed");
        }
        var fnEnv = new FnEnv(env,NumberInfo.INT);
        fnEnv.addFunction("argc",new Variable(NumberInfo.INT,"argc","argc"));
        fnEnv.addFunction("args",new Variable(new PointerInfo(modEnv.getFPL().getString()),"args","args"));
        fnEnv.addFunction("mainThread",new Variable(modEnv.getType(new Atom(0,0,"Thread",AtomType.ID)),"mainThread","mainThread"));
        var b = new BuilderWriter();
        b.write(modEnv.getInitializer());
        var tmp = new BuilderWriter();
        it.nextList().compile(tmp,fnEnv,it);
        fnEnv.compileToPointerVars(b);
        b.write(tmp.getCode());
        fnEnv.compileDestructorCalls(b);
        b.write(modEnv.getDestructor());
        if(fnEnv.isReturnNotUsed()){
           b.write("onExit();\nreturn 0;\n");
        }
        modEnv.declareMain();
        modEnv.getModuleBlock().setMainFunctionCode(b.getCode());
        return TypeInfo.VOID;
    }
}