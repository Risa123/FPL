package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class SetElement extends AField{
	private final TypeInfo valueType;
	public SetElement(TypeInfo valueType){
		this.valueType = valueType;
	}
	@Override
	public TypeInfo
	compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
		writePrev(writer);
		writer.write('[');
		int beginChar = 0;
		var indexExp = it.next();
		var tmpWriter = new BuilderWriter(writer);
	    var indexType = indexExp.compile(tmpWriter,env,it);
	    if(indexType.notIntegerNumber()){
	    	throw new CompilerException(indexExp,"integer number expected");
	    }
	    var code = tmpWriter.getCode();
	    writer.write(code);
		writer.write("]=");
		var valueType = it.next().compile(writer,env,it);
	    if(!this.valueType.equals(valueType)){
	    	throw new CompilerException(line,beginChar,this.valueType + " return type expected instead of " + valueType);
		}
		return TypeInfo.VOID;
	}
}