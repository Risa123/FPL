package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.ModuleBlock;
import risa.fpl.env.AEnv;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public abstract class AThreePassBlock{
    public static final int MAX_PASSES = 3;//three passes necessary in some cases
    protected final void compile(BufferedWriter writer,AEnv env,List block)throws CompilerException,IOException{
        var infos = new ArrayList<ExpressionInfo>(block.getExps().size());
        for(var exp:block.getExps()){
            infos.add(new ExpressionInfo(exp));
        }
        compile(writer,env,infos);
    }
    protected final void compile(BufferedWriter writer,AEnv env,ArrayList<ExpressionInfo>infos)throws CompilerException,IOException{
        for(int i = 0; i < MAX_PASSES && !infos.isEmpty();++i){
            var it = infos.iterator();
            while(it.hasNext()){
                var info = it.next();
                try{
                    info.getExp().compile(info.getWriter(),env,null);
                    it.remove();
                    writer.write(info.getWriter().getCode());
                }catch(CompilerException e){
                    var exps = ((List)info.getExp()).getExps();
                    if(!exps.isEmpty() && exps.get(0) instanceof Atom a && a.getValue().equals("use") && this instanceof ModuleBlock){
                        throw e;
                    }
                    info.setLastEx(e);
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
                info.getLastEx().setSourceFile("");
                b.append('\n').append(info.getLastEx().getMessage());
            }
            var first = infos.get(0).getExp();
            throw new CompilerException(first.getLine(),first.getTokenNum(),b.toString());
        }
    }
}