package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public class ValueExp extends AField {
    protected final TypeInfo type;
    protected final String code;
    public ValueExp(TypeInfo type,String code,AccessModifier accessModifier) {
        super(accessModifier);
    	this.type = type;
    	this.code = code;
    }
    public ValueExp(TypeInfo type,String code){
        this(type,code,AccessModifier.PUBLIC);
    }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
		if(it.hasNext()) {
			if(it.peek() instanceof Atom atom) {
				if(atom.getType() == TokenType.ARG_SEPARATOR) {
					it.next();
				}else if(atom.getType() != TokenType.END_ARGS){
					it.next();
					return onField(atom,writer,env,it,line,charNum);
				}
			}
		}
        writePrev(writer);
		writer.write(code);
		return type;
	}
	protected TypeInfo onField(Atom atom,BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws CompilerException,IOException{
		var field = type.getField(atom.getValue(),env);
		if(field == null) {
			throw new CompilerException(atom,type + " has no field  called " + atom);
		}
		var selector = "";
		var prefix = "";
		if(field instanceof Variable) {
		   if(type instanceof PointerInfo) {
			   selector = "->";
		   }else {
			   selector = ".";
		   }
		}else if(field instanceof ICalledOnPointer f && type instanceof PointerInfo){
            f.calledOnPointer();
        }
		if(getPrevCode() != null){
		    prefix = getPrevCode() + prefix;
        }
		field.setPrevCode(prefix + code + selector);
		return field.compile(writer,env,it,line,charNum);
	}
}