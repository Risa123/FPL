package risa.fpl.function.statement;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.exp.AField;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public final class ParenthExp extends AField{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var list = new ArrayList<AExp>();
        var notIn = true;
        while(it.hasNext()){
            var exp = it.nextAtom();
            if(exp.getType() == TokenType.ID){
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
        var b = new BuilderWriter(writer);
        b.write('(');
        var ret = new List(line,tokenNum,list,false).compile(b,env,it);
        b.write(')');
        return compileChainedCall(ret,writer,env,it,b.getCode());
    }
}