package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Enum implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var a = it.nextID();
        var id = a.getValue();
        if(env.hasFunctionInCurrentEnv(id)){
            throw new CompilerException(a,"there is already function called " + id);
        }
        var type = new TypeInfo(id,"unsigned int");
        var c = new ClassInfo(id);
        type.setClassInfo(c);
        var i = 0;
        while(it.hasNext()){
            var t = it.nextAtom();
            if(t.getType() == TokenType.NEW_LINE){
                break;
            }else if(t.getType() == TokenType.ID){
                if(c.getField(t.getValue(),env) != null){
                    throw new CompilerException(t,"value "  + t + " is already declared");
                }
                c.addField(t.getValue(),new ValueExp(type,Integer.toString(i)));
                i++;
            }else{
                throw new CompilerException(t,"identifier expected");
            }
        }
        env.addType(id,type);
        return TypeInfo.VOID;
    }
}