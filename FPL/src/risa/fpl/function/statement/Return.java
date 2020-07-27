package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Return implements IFunction {

	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		writer.write("return ");
		TypeInfo returnType;
		var subEnv = (SubEnv)env;
		if(it.hasNext()) {
			var exp = it.next();
		    returnType = exp.compile(writer, env,it);
			if(!subEnv.getReturnType().equals(returnType)) {
				throw new CompilerException(exp,returnType + " cannot be implicitly converted to " + subEnv.getReturnType());
			}
		}else {
			if(subEnv.getReturnType() != TypeInfo.VOID){
				throw new CompilerException(line,charNum,"this function doesnt return void");
			}
		}
		return TypeInfo.VOID;
	}

}