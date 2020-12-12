package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Asm implements IFunction {
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var code = it.nextAtom();
        if(code.getType() != TokenType.STRING){
            throw new CompilerException(code,"assembly code as string literal expected");
        }
        writer.write("asm(" + code.getValue() + ");\n");
        return TypeInfo.VOID;
    }
}