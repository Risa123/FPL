package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class UnaryOperator extends AField{
	private final TypeInfo returnType;
	private final String operator;
	private final boolean postfix;
	public UnaryOperator(TypeInfo returnType,String operator,boolean postfix){
		this.returnType = returnType;
		this.operator = operator;
		this.postfix = postfix;
	}
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
	    var prev = new StringBuilder();
		if(postfix){
			writePrev(prev);
		}
		prev.append(operator);
		if(!postfix){
			writePrev(prev);
		}
		return compileChainedCall(returnType,builder,env,it,prev.toString());
	}
}