package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public final class SetElement extends AField {
	private final TypeInfo valueType;
	public SetElement(TypeInfo valueType){
		this.valueType = valueType;
	}
	@Override
	public TypeInfo
	compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
		writePrev(writer);
		writer.write('[');
		var list = new ArrayList<AExp>();
		var first = true;
		int beginChar = 0;
		while(it.hasNext()){
			var exp = it.next();
			if(first){
				first = false;
				beginChar = exp.getLine();
			}
			if(exp instanceof Atom a && a.getType() == TokenType.ARG_SEPARATOR){
				break;
			}
			list.add(exp);
		}
		var indexExp = new List(line,beginChar,list,true);
		var tmpWriter = new BuilderWriter(writer);
	    var indexType = indexExp.compile(tmpWriter,env,it);
	    if(!indexType.isIntegerNumber()) {
	    	throw new CompilerException(indexExp,"integer number expected");
	    }
	    var code = tmpWriter.getCode();
	    writer.write(code.substring(0,code.length() - 2));
		writer.write("]=");
	    var vList = new ArrayList<AExp>();
	    first = true;
	    while(it.hasNext()){
	       var exp = it.next();
	       if(first){
	       	  first = false;
	       	  beginChar = exp.getCharNum();
		   }
	       if(exp instanceof Atom a && a.getType() == TokenType.END_ARGS){
	       	   break;
		   }
           vList.add(exp);
		}
		var valueType = new List(line,beginChar,vList,true).compile(writer,env,it);
	    if(!this.valueType.equals(valueType)){
	    	throw new CompilerException(line,beginChar,this.valueType + " return type expected instead of " + valueType);
		}
		return TypeInfo.VOID;
	}
}