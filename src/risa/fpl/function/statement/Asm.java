package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Asm implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var code = it.nextAtom();
        if(code.getType() != AtomType.STRING){
            throw new CompilerException(code,"assembly code as string expected");
        }
        writer.write("asm(" + code + ")");
        return TypeInfo.VOID;
    }
}