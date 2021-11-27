package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
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
	public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var prev = new BuilderWriter();
		writePrev(prev);
		prev.write('[');
		var indexExp = it.next();
		var indexType = indexExp.compile(prev,env,it);
		if(indexType.notIntegerNumber()){
		    throw new CompilerException(indexExp,"integer number expected");
        }
		prev.write(']');
		return compileChainedCall(returnType,writer,env,it,prev.getCode());
	}
}