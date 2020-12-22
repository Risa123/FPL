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
        writer.write("asm( \"push %rax\");\n");
        writer.write("asm( \"push %rcx\"");
        writer.write("asm( \"push %rdx\");\n");
        writer.write("asm( \"push %rbx\");\n");
        writer.write("asm( \"push %rbp\");\n");
        writer.write("asm( \"push %rbp\");\n");
        writer.write("if(){\n");
        it.nextList().compile(writer,env,it);
        var hasFin = false;
        while(it.hasNext()){
            var exp = it.peek();
            if(exp instanceof Atom blockName && blockName.getType() == TokenType.ID){
                if(blockName.getValue().equals("catch")){
                    if(hasFin){
                        throw new CompilerException(exp,"catch can only come before finally");
                    }
                    it.next();
                    var expType = it.nextID();
                    var expName = it.nextID();
                    it.nextList().compile(writer,env,it);
                }else if(blockName.getValue().equals("finally")){
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
        return TypeInfo.VOID;
    }
}