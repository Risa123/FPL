package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class GetIndex extends AField {
    private final TypeInfo returnType;
    public GetIndex(TypeInfo returnType) {
    	this.returnType = returnType;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		writePrev(writer);
		writer.write('[');
		var indexAtom = it.nextAtom();
		var indexType = env.getFunction(indexAtom).compile(writer, env, it, line, charNum);
		if(!indexType.isIntegerNumber()){
		    throw new CompilerException(indexAtom,"integer number expected");
        }
		writer.write(']');
		return returnType;
	}
}