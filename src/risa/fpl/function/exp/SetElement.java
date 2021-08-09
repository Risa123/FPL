package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class SetElement extends AField{
	private final TypeInfo valueType;
	public SetElement(TypeInfo valueType){
		this.valueType = valueType;
	}
	@Override
	public TypeInfo
	compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		writePrev(writer);
		writer.write('[');
		int beginChar = 0;
		var indexExp = it.next();
		var tmpWriter = new BuilderWriter();
	    var indexType = indexExp.compile(tmpWriter,env,it);
	    if(indexType.notIntegerNumber()){
	    	throw new CompilerException(indexExp,"integer number expected");
	    }
	    var code = tmpWriter.getCode();
	    writer.write(code);
		writer.write("]=");
		var valueAtom = it.nextAtom();
		if(valueAtom.getType() == TokenType.ARG_SEPARATOR){
			valueAtom = it.nextAtom();
		}
		var valueType = valueAtom.compile(writer,env,it);
	    if(!this.valueType.equals(valueType)){
	    	throw new CompilerException(line,beginChar,this.valueType + " return type expected instead of " + valueType);
		}
		return TypeInfo.VOID;
	}
}