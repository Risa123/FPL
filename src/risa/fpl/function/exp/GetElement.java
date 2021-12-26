package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class GetElement extends AField{
    private final TypeInfo returnType;
    public GetElement(TypeInfo returnType){
    	this.returnType = returnType;
    }
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        var prev = new StringBuilder();
		writePrev(prev);
		prev.append('[');
		var indexExp = it.next();
		var indexType = indexExp.compile(prev,env,it);
		if(indexType.notIntegerNumber()){
		    throw new CompilerException(indexExp,"integer number expected");
        }
		prev.append(']');
		return compileChainedCall(returnType,builder,env,it,prev.toString());
	}
}