package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
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
	public Variable(TypeInfo type, String code, boolean onlyDeclared, String id, boolean constant, boolean classAttribute, AccessModifier mod) {
		super(type, code,mod);
		this.onlyDeclared = onlyDeclared;
		this.id = id;
		this.constant = constant;
		this.classAttribute = classAttribute;
	}
    public Variable(TypeInfo type,String code,String id) {
       this(type,code,false,id,false,false,AccessModifier.PUBLIC);
    }
	@Override
	protected TypeInfo onField(Atom atom, BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws CompilerException, IOException {
	    var value = atom.getValue();
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
		}else if(type instanceof PointerInfo p && !p.isFunctionPointer()){
		    TypeInfo t;
           if((t = processOperator(value,writer,it,env)) != null){
               return t;
           }
        }else if(type instanceof NumberInfo) {
			if(constant) {
				throw new CompilerException(line,charNum,"constant cannot be redefined");
			}
			TypeInfo t;
			if((t = processOperator(value,writer,it,env)) != null){
			    return t;
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
	private TypeInfo processOperator(String operator,BufferedWriter writer,ExpIterator it,AEnv env) throws IOException, CompilerException {
            switch (operator) {
                case "+=", "-=", "/=", "*=" -> {
                    process(operator,writer,it,env);
                    return TypeInfo.VOID;
                }
                case "++","--","p+","p-" ->{
                    writer.write(code);
                    if(operator.equals("p+")){
                        operator = "++";
                    }else if(operator.equals("p-")){
                        operator = "--";
                    }
                    writer.write(operator);
                    return type;
                }
            }
            if(operator.equals("%=") && (type instanceof NumberInfo n && !n.floatingPoint || type instanceof PointerInfo)){
                process(operator,writer,it,env);
                return TypeInfo.VOID;
            }else if(type instanceof PointerInfo && operator.equals("drf=")){
                writer.write('*');
                process("=",writer,it,env);
                return TypeInfo.VOID;
            }
	    return null;
    }
    private void process(String operator,BufferedWriter writer,ExpIterator it,AEnv env) throws IOException, CompilerException {
        writer.write(code);
        writer.write(operator);
        it.nextAtom().compile(writer, env, it);
    }
}