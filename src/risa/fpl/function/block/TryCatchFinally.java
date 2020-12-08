package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class TryCatchFinally extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        it.nextList().compile(writer,env,it);
        while(it.hasNext()){
            var exp = it.peek();
            if(exp instanceof Atom blockName && blockName.getType() == TokenType.ID){
                if(blockName.getValue().equals("catch")){
                    it.next();
                    var expType = it.nextID();
                    var expName = it.nextID();
                    it.nextList().compile(writer,env,it);
                }else if(blockName.getValue().equals("finally")){
                    it.next();
                    it.nextList().compile(writer,env,it);
                }else{
                    break;
                }
            }else{
                break;
            }
        }
        return TypeInfo.VOID;
    }
}