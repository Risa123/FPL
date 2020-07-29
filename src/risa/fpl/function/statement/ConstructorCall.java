package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class ConstructorCall extends Function {
   private final TypeInfo type;
   public ConstructorCall(TypeInfo type,TypeInfo[]args) {
       super("constructor",TypeInfo.VOID,type.cname + "__init",args,false,type);
	   this.type = type;
   }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		writer.write(type.cname);
		writer.write(' ');
		var id = it.nextID();
		var cID = IFunction.toCId(id.value);
		writer.write(cID);
		writer.write(";\n");
		setPrevCode(cID);
		super.compile(writer,env,it,line,charNum);
		env.addFunction(id.value,new Variable(type,IFunction.toCId(id.value),id.value));
		return TypeInfo.VOID;
	}

}