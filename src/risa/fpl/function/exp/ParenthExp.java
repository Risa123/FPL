package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

import java.util.ArrayList;

public final class ParenthExp extends AField{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        var list = new ArrayList<AExp>();
        var notIn = true;
        while(it.hasNext()){
            var exp = it.nextAtom();
            if(exp.getType() == AtomType.ID){
                if(exp.getValue().equals("[")){
                    notIn = false;
                }else if(exp.getValue().equals("]")){
                    if(notIn){
                        break;
                    }else{
                        notIn = true;
                    }
                }
            }
            list.add(exp);
        }
        var b = new StringBuilder();
        b.append('(');
        var ret = new List(line,tokenNum,list,false).compile(b,env,it);
        b.append(')');
        return compileChainedCall(ret,builder,env,it,b.toString());
    }
}