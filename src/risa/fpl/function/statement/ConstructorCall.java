package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class ConstructorCall implements IFunction {
   private final TypeInfo type;
   public ConstructorCall(TypeInfo type) {
	   this.type = type;
   }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		writer.write(type.cname);
		writer.write(' ');
		var id = it.nextID();
		writer.write(IFunction.toCId(id.value));
		env.addFunction(id.value,new Variable(type,IFunction.toCId(id.value),false,id.value,false));
		return TypeInfo.VOID;
	}

}