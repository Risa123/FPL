package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public class BinaryOperator extends AField {
   private final TypeInfo returnType,operandType;
   private final String operator;
   public BinaryOperator(TypeInfo returnType,TypeInfo operandType,String operator) {
	   this.operandType = operandType;
	   this.returnType = returnType;
	   this.operator = operator;
   }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
	    writePrev(writer);
	    writer.write(operator);	
	    var exp = it.next();
	    var opType = exp.compile(writer, env,it);
	    if(!operandType.equals(opType)) {
	    	throw new CompilerException(exp,operandType + " operand expected instead of " + opType);
	    }
		return returnType;
	}
}