package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.statement.ConstructorCall;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class ClassBlock implements IFunction {

	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		var id = it.nextID();
		var cEnv = new ClassEnv(env);
		var b = new BuilderWriter(writer);
		b.write("typedef struct ");
		var cID = IFunction.toCId(id.value);
	    b.write(IFunction.toCId(id.value));
	    b.write("{\n");
		it.nextList().compile(b,cEnv, it);
	    b.write('}');
	    b.write(IFunction.toCId(id.value));
	    b.write(";\n");
	    writer.write(b.getText());
	    var type = new TypeInfo(id.value,cID,b.getText());
	    cEnv.addFields(type);
	    
	    env.addType(id.value,type);
	    env.addFunction(id.value,new ConstructorCall(type));
		return TypeInfo.VOID;
	}

}