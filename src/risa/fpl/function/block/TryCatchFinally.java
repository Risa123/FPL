package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class TryCatchFinally extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var count = ((FnSubEnv)env).getCatchNum();
        var catchLabel = "catch" + count;
        var catchEndLabel = "catch_end" + count;
        writer.write("_std_lang_currentThread->_throwTarget=&&" + catchLabel + ";\n");
        it.nextList().compile(writer,env,it);
        writer.write("goto " + catchEndLabel + ";\n");
        var hasFin = false;
        while(it.hasNext()){
            var exp = it.peek();
            if(exp instanceof Atom blockName && blockName.getType() == TokenType.ID){
                if(blockName.getValue().equals("catch")){
                    writer.write(catchLabel + ":\n");
                    if(hasFin){
                        throw new CompilerException(exp,"catch can only come before finally");
                    }
                    it.next();
                    var expType = it.nextID();
                    var expName = it.nextID();
                    it.nextList().compile(writer,env,it);
                }else if(blockName.getValue().equals("finally")){
                    writer.write(catchEndLabel + ":\n");
                    if(hasFin){
                        throw new CompilerException(blockName,"multiple declarations of finally");
                    }
                    hasFin = true;
                    it.next();
                    it.nextList().compile(writer,env,it);
                }else{
                    break;
                }
            }else{
                break;
            }
        }
        if(!hasFin){
            writer.write(catchEndLabel + ":\n");
        }
        return TypeInfo.VOID;
    }
}