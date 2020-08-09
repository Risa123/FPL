package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class SetIndex extends AField {
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum)throws IOException, CompilerException {
		writePrev(writer);
		writer.write('[');
		var indexExp = it.nextAtom();
	    var indexType = env.getFunction(indexExp).compile(writer, env, it, line, charNum);
	    if(!indexType.isIntegerNumber()) {
	    	throw new CompilerException(indexExp,"integer number expected");
	    }
		writer.write("]=");
		it.nextAtom().compile(writer, env, it);
		return TypeInfo.VOID;
	}
}