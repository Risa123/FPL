package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

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
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        if(it.hasNext() && it.peek() instanceof Atom atom && atom.getType() != AtomType.END_ARGS && atom.getType() != AtomType.ARG_SEPARATOR){
			it.next();
			return onField(atom,builder,env,it,line,tokenNum);
        }
        writePrev(builder);
		builder.append(code);
		return type;
	}
	protected TypeInfo onField(Atom atom,StringBuilder builder,SubEnv env,ExpIterator it,int line,int charNum)throws CompilerException{
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
			code = i.getToPointerName() + '(' + code + ",&"+ env.getToPointerVarName(i) + ')';
			f.calledOnReturnedInstance();
		}
		field.setPrevCode(prefix + code + selector);
		return field.compile(builder,env,it,line,charNum);
	}
}