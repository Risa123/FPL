package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Throw extends AField {
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        writePrev(writer);
        writer.write(";\nvoid longjmp(jmp_buf env,int value);\n");
        writer.write("longjmp(_std_lang_currentThread->_env,_std_lang_currentThread->_id);\n");
        return TypeInfo.VOID;
    }
}