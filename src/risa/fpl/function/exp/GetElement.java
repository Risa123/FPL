package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public final class GetElement extends AField {
    private final TypeInfo returnType;
    public GetElement(TypeInfo returnType) {
    	this.returnType = returnType;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum) throws IOException, CompilerException {
		writePrev(writer);
		writer.write('[');
		var list = new ArrayList<AExp>();
		while(it.hasNext()){
			var exp = it.next();
			if(exp instanceof Atom a && a.getType() == TokenType.END_ARGS){
				break;
			}
			list.add(exp);
		}
		var indexExp = new List(line,charNum,list,false);
		var indexType = indexExp.compile(writer,env,it);
		if(indexType.notIntegerNumber()){
		    throw new CompilerException(indexExp,"integer number expected");
        }
		writer.write(']');
		return returnType;
	}
}