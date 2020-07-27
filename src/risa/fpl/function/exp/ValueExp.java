package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public class ValueExp extends AField {
    protected final TypeInfo type;
    protected final String code;
    public ValueExp(TypeInfo type,String code) {
    	this.type = type;
    	this.code = code;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		writePrev(writer);
		if(it.hasNext()) {
			var exp = it.peek();
			if(exp instanceof Atom atom) {
				if(atom.type == TokenType.ARG_SEPARATOR) {
					it.next();
				}else if(atom.type == TokenType.END_ARGS){

				}else {
					it.next();
					return onField(atom,writer,env,it,line,charNum);
				}
			}else {
				throw new CompilerException(exp,"unexpected list");
			}
		}
		writer.write(code);
		return type;
	}
	protected TypeInfo onField(Atom atom,BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum) throws CompilerException, IOException {
		var field = type.getField(atom.value);
		if(field == null) {
			throw new CompilerException(atom,type + " has no field  called " + atom.value);
		}
		var selector = "";
		if(field instanceof Variable) {
		   if(type instanceof PointerInfo) {
			   selector = "->";
		   }else {
			   selector = ".";
		   }
		}
		field.prev_code = code + selector;
		return field.compile(writer, env, it, line, charNum);
	}
}