package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Cast extends AField {
	private final TypeInfo target;
    public Cast(TypeInfo target) {
    	this.target = target;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
	    var type = env.getType(it.nextID());
	    if(!((type instanceof NumberInfo || type instanceof PointerInfo) && (target instanceof NumberInfo || target instanceof PointerInfo))) {
	    	throw new CompilerException(line,charNum,"conversion is only possible between pointers and numbers");
	    }
	    writer.write('(');
	    writer.write(type.getCname());
	    writer.write(')');
		writePrev(writer);
		return type;
	}

}