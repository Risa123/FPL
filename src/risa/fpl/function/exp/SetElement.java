package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

public final class SetElement extends AField{
	private final TypeInfo valueType;
	public SetElement(TypeInfo valueType){
		this.valueType = valueType;
	}
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		writePrev(builder);
		builder.append('[');
		int beginChar = 0;
		var indexExp = it.next();
		var tmpBuilder = new StringBuilder();
	    var indexType = indexExp.compile(tmpBuilder,env,it);
	    if(indexType.notIntegerNumber()){
	    	error(indexExp,"integer number expected");
	    }
	    var code = tmpBuilder.toString();
	    builder.append(code).append("]=");
		var valueAtom = it.nextAtom();
		if(valueAtom.getType() == AtomType.ARG_SEPARATOR){
			valueAtom = it.nextAtom();
		}
		var tmp = new StringBuilder();
		var valueType = valueAtom.compile(tmp,env,it);
		var callCopy = valueType instanceof InstanceInfo i && i.getCopyConstructorName() != null;
		if(callCopy){
			builder.append(((InstanceInfo)valueType).getCopyConstructorName()).append("AndReturn(");
		}else if(valueType instanceof InterfaceInfo i){
			builder.append(i.getCopyName()).append("AndReturn(");
			callCopy = true;
		}
		builder.append(tmp);
		if(callCopy){
			builder.append(')');
		}
	    if(!this.valueType.equals(valueType)){
	    	error(line,beginChar,this.valueType + " return type expected instead of " + valueType);
		}
		return TypeInfo.VOID;
	}
}