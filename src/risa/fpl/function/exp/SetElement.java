package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
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
		var valueType = valueAtom.compile(builder,env,it);
	    if(!this.valueType.equals(valueType)){
	    	error(line,beginChar,this.valueType + " return type expected instead of " + valueType);
		}
		return TypeInfo.VOID;
	}
}