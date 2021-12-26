package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

public final class BinaryOperator extends AField{
   private final TypeInfo returnType,operandType;
   private final String operator;
   public BinaryOperator(TypeInfo returnType,TypeInfo operandType,String operator){
	   this.operandType = operandType;
	   this.returnType = returnType;
	   this.operator = operator;
    }
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		var returnType = this.returnType;
	    if(it.hasNext() && it.peek() instanceof Atom a && a.getType() != AtomType.END_ARGS && a.getType() != AtomType.ARG_SEPARATOR){
			var exp = it.next();
			writePrev(builder);
			builder.append(operator);
			var opType = exp.compile(builder,env,it);
			if(returnType instanceof NumberInfo && opType instanceof NumberInfo on){
				if(on.isFloatingPoint()){
					returnType = on;
				}
			}else if(!operandType.equals(opType) && !(operandType instanceof NumberInfo && opType instanceof NumberInfo && returnType == TypeInfo.BOOL)){
				throw new CompilerException(exp,operandType + " operand expected instead of " + opType);
			}
		}else if(!(operator.equals("+") || operator.equals("-"))){
	    	throw new CompilerException(line,tokenNum,"atom expected");
		}else{
	    	builder.append(operator);
	    	writePrev(builder);
		}
	    var prevCode = getPrevCode();
	    if(prevCode == null){
	        prevCode = "";
        }
		return compileChainedCall(returnType,builder,env,it,prevCode);
	}
}