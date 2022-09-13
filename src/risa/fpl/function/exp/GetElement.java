package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.InstanceInfo;
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
		var copyCalled = false;
		if(returnType instanceof InstanceInfo i && i.getCopyConstructorName() != null){
			prev.append(i.getCopyConstructorName()).append("AndReturn(");
			copyCalled = true;
		}
		writePrev(prev);
		prev.append('[');
		var indexExp = it.next();
		var indexType = indexExp.compile(prev,env,it);
		if(indexType.notIntegerNumber()){
		    error(indexExp,"integer number expected");
        }
		prev.append(']');
		if(copyCalled){
			prev.append(')');
		}
		return compileChainedCall(returnType,builder,env,it,prev.toString());
	}
}