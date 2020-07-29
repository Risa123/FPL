package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;

public class Variable extends ValueExp {
	public boolean onlyDeclared;
	private final String id;
	public final boolean constant;
	private final boolean classAttribute;
	public Variable(TypeInfo type, String code,boolean onlyDeclared,String id,boolean constant,boolean classAttribute) {
		super(type, code);
		this.onlyDeclared = onlyDeclared;
		this.id = id;
		this.constant = constant;
		this.classAttribute = classAttribute;
	}
    public Variable(TypeInfo type,String code,String id) {
       this(type,code,false,id,false,false);
    }
	@Override
	protected TypeInfo onField(Atom atom, BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws CompilerException, IOException {
	    var value = atom.value;
		if(value.equals("=")) {
		   if(constant) {
			  throw new CompilerException(line,charNum,"constant cannot be redefined");    	
			}
		    writer.write(code);
			writer.write('=');
			onlyDeclared = false;
		    it.nextAtom().compile(writer, env, it);
		    return TypeInfo.VOID;
		}else if(value.equals("&")) {
			writer.write('&');
			writer.write(code);
			return new PointerInfo(type);
		}else if(type instanceof NumberInfo n) {
			if(constant) {
				throw new CompilerException(line,charNum,"constant cannot be redefined");
			}
			switch(value) {
			case "+=":
			case "-=":
			case "*=":
			case "/=":
			  writer.write(code);
		      writer.write(value);
		      it.nextAtom().compile(writer, env, it);
			  return TypeInfo.VOID;	
			  default:
				  if(!n.floatingPoint && value.equals("%=")) {
					  writer.write(value);
					  it.nextAtom().compile(writer, env, it);
				  }
			}
		}
		return super.onField(atom, writer, env, it, line, charNum);
	}

	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		if(onlyDeclared) {
			throw new CompilerException(line,charNum,"variable " + id + " was not defined");
		}
		if(getPrevCode() == null && classAttribute){
		    writer.write("this->");
        }
		return super.compile(writer, env, it, line, charNum);
	}
	
}