package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class BinaryOperator extends AField{
   private final TypeInfo returnType,operandType;
   private final String operator;
   public BinaryOperator(TypeInfo returnType,TypeInfo operandType,String operator){
	   this.operandType = operandType;
	   this.returnType = returnType;
	   this.operator = operator;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		var returnType = this.returnType;
	    if(it.hasNext() && it.peek() instanceof Atom a && a.getType() != TokenType.END_ARGS && a.getType() != TokenType.ARG_SEPARATOR){
			var exp = it.next();
			writePrev(writer);
			writer.write(operator);
			var opType = exp.compile(writer,env,it);
			if(returnType instanceof NumberInfo && opType instanceof NumberInfo on){
				if(on.isFloatingPoint()){
					returnType = on;
				}
			}else if(!operandType.equals(opType)){
				throw new CompilerException(exp,operandType + " operand expected instead of " + opType);
			}
		}else if(!(operator.equals("+") || operator.equals("-"))){
	    	throw new CompilerException(line, tokenNum,"atom expected");
		}else{
	    	writer.write(operator);
	    	writePrev(writer);
		}
	    var prevCode = getPrevCode();
	    if(prevCode == null){
	        prevCode = "";
        }
		return compileChainedCall(returnType,writer,env,it,prevCode);
	}
}