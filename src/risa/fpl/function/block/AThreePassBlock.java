package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.ModuleBlock;
import risa.fpl.env.SubEnv;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;

import java.util.ArrayList;

public abstract class AThreePassBlock{
    public static final int MAX_PASSES = 3;//three passes necessary in some cases
    protected final void compile(StringBuilder builder,SubEnv env,ArrayList<ExpressionInfo>infos)throws CompilerException{
        for(int i = 0; i < MAX_PASSES && !infos.isEmpty();++i){
            var it = infos.iterator();
            while(it.hasNext()){
                var info = it.next();
                try{
                    info.getExp().compile(info.getBuilder(),env,null);
                    it.remove();
                    builder.append(info.getBuilder());
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
            b.append(this instanceof ClassBlock?"class block":(this instanceof ModuleBlock?"module block":"interface block")).append(':');
            for(var info:infos){
                b.append('\n').append(info.getLastEx().getMessage());
            }
            throw new CompilerException(infos.get(0).getExp(),b.toString());
        }
    }
    protected final ArrayList<ExpressionInfo>createInfoList(List from){
        var list = new ArrayList<ExpressionInfo>(from.getExps().size());
        for(var exp:from.getExps()){
            list.add(new ExpressionInfo(exp));
        }
        return list;
    }
}