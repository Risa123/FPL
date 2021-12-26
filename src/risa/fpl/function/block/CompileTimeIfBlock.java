package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.ANameSpacedEnv;
import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

import java.io.BufferedWriter;
import java.io.IOException;

public class CompileTimeIfBlock extends AThreePassBlock implements IFunction{
    private final List list;
    public CompileTimeIfBlock(List list){
        this.list = list;
    }
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        if(env instanceof ANameSpacedEnv e){
           var infos = e.getCompileBlockInfos(line);
           if(infos == null){
               infos = createInfoList(list);
               e.addCompileIfBlockInfos(line,infos);
           }
           compile(builder,env,infos);
        }else{
            list.compile(builder,env,it);
        }
        return TypeInfo.VOID;
    }
}