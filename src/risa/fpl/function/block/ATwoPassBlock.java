package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.ModuleBlock;
import risa.fpl.env.AEnv;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public abstract class ATwoPassBlock{
    protected final void compile(BufferedWriter writer,AEnv env,List list)throws CompilerException,IOException{
        var infos = new ArrayList<ExpInfo>(list.getExps().size());
        for(var exp:list.getExps()){
            var info = new ExpInfo();
            info.exp = exp;
            info.writer = new BuilderWriter(writer);
            infos.add(info);
        }
        for(int i = 0; i < 2 && !infos.isEmpty();++i){
            var it = infos.iterator();
            while(it.hasNext()){
                var info = it.next();
                try {
                    info.exp.compile(info.writer,env,null);
                    it.remove();
                    writer.write(info.writer.getCode());
                }catch(CompilerException e){
                    var exps = ((List)info.exp).getExps();
                    if(!exps.isEmpty() && exps.get(0) instanceof Atom a && a.getValue().equals("use") && this instanceof ModuleBlock){
                        throw e;
                    }
                    info.lastEx = e;
                    info.writer = new BuilderWriter(writer);
                }
            }
        }
        if(!infos.isEmpty()){
            var b = new StringBuilder("errors in ");
            if(this instanceof ClassBlock){
                b.append("class block");
            }else{
                b.append("module block");
            }
            b.append(':');
            for(var info:infos){
                b.append('\n');
                info.lastEx.setSourceFile("");
                b.append(info.lastEx.getMessage());
            }
            var first = infos.get(0).exp;
            throw new CompilerException(first.getLine(),first.getCharNum(),b.toString());
        }
    }
    private static class ExpInfo{
        AExp exp;
        CompilerException lastEx;
        BuilderWriter writer;
    }
}