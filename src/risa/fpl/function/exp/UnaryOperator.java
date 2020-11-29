package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class UnaryOperator extends AField {
	private final TypeInfo returnType;
	private final String operator;
	private final boolean postfix;
	public UnaryOperator(TypeInfo returnType,String operator,boolean postfix) {
		this.returnType = returnType;
		this.operator = operator;
		this.postfix = postfix;
	}
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNm)throws IOException,CompilerException {
	    var prev = new BuilderWriter(writer);
		if(postfix) {
			writePrev(prev);
		}
		prev.write(operator);
		if(!postfix) {
			writePrev(prev);
		}
		return compileChainedCall(returnType,writer,env,it,prev.getCode());
	}
}