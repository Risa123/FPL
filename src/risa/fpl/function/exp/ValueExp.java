package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public class ValueExp extends AField{
    protected final TypeInfo type;
    protected final String code;
    public ValueExp(TypeInfo type,String code,AccessModifier accessModifier){
        super(accessModifier);
    	this.type = type;
    	this.code = code;
    }
    public ValueExp(TypeInfo type,String code){
        this(type,code,AccessModifier.PUBLIC);
    }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        if(it.hasNext() && it.peek() instanceof Atom atom && atom.getType() != TokenType.END_ARGS && atom.getType() != TokenType.ARG_SEPARATOR){
			it.next();
			return onField(atom,writer,env,it,line, tokenNum);
        }
        writePrev(writer);
		writer.write(code);
		return type;
	}
	protected TypeInfo onField(Atom atom,BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws CompilerException,IOException{
		var field = type.getField(atom.getValue(),env);
		if(field == null){
			throw new CompilerException(atom,type + " has no field called " + atom);
		}
		var selector = "";
		var prefix = "";
		if(field instanceof Variable){
		   if(type instanceof PointerInfo){
			   selector = "->";
		   }else{
			   selector = ".";
		   }
		}else if(field instanceof ICalledOnPointer f && type instanceof PointerInfo){
            f.calledOnPointer();
        }
		if(getPrevCode() != null){
		    prefix = getPrevCode();
		    setPrevCode(null);
        }
		var code = this.code;
		if(!(this instanceof Variable) && field instanceof Function f && type instanceof InstanceInfo i){
			code = i.getToPointerName() + "(" + code + ")";
			f.calledOnReturnedInstance();
		}
		field.setPrevCode(prefix + code + selector);
		return field.compile(writer,env,it,line,charNum);
	}
}