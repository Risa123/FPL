package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public final class Variable extends ValueExp {
	private boolean onlyDeclared;
	private final String id;
	private final boolean constant;
	private final TypeInfo instanceType;
	public Variable(TypeInfo type, String code, boolean onlyDeclared, String id, boolean constant,TypeInfo instanceType, AccessModifier mod) {
		super(type, code,mod);
		this.onlyDeclared = onlyDeclared;
		this.id = id;
		this.constant = constant;
		this.instanceType = instanceType;
	}
    public Variable(TypeInfo type,String code,String id) {
       this(type,code,false,id,false,null,AccessModifier.PUBLIC);
    }
	@Override
	protected TypeInfo onField(Atom atom, BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws CompilerException,IOException{
	    var value = atom.getValue();
		if(value.equals("=")) {
		   if(constant) {
			  throw new CompilerException(line,charNum,"constant cannot be redefined");    	
			}
		    writePrev(writer);
		    writer.write(code);
			writer.write('=');
			onlyDeclared = false;
		    execute(it,writer,env);
		    return TypeInfo.VOID;
		}else if(value.equals("ref")) {
            writer.write('&');
            writePrev(writer);
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
		return super.onField(atom,writer,env,it,line,charNum);
	}

	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException,CompilerException{
		if(onlyDeclared && it.hasNext() && it.peek() instanceof Atom a && !a.getValue().endsWith("=")){
		    throw new CompilerException(line,charNum,"variable " + id + " not defined");
        }
		if(instanceType != null && getPrevCode() == null){
		    setPrevCode("((" + instanceType.getCname() + "*)this)->");
        }
		return super.compile(writer,env,it,line,charNum);
	}
	private TypeInfo processOperator(String operator,BufferedWriter writer,ExpIterator it,AEnv env) throws IOException,CompilerException{
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
            if(operator.equals("%=") && (type instanceof NumberInfo n && !n.isFloatingPoint() || type instanceof PointerInfo)){
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
        execute(it,writer,env);
    }
    private void execute(ExpIterator it,BufferedWriter writer,AEnv env) throws CompilerException, IOException {
	    var list = new ArrayList<AExp>();
	    var first = it.nextAtom();
	    list.add(first);
	    while(it.hasNext()){
	        list.add(it.next());
        }
	    new List(first.getLine(),first.getCharNum(),list,true).compile(writer,env,it);
    }
    public TypeInfo getType(){
	    return type;
    }
    public String getExternDeclaration(){
	    return "extern " + type.getCname() +  " " + code + ";\n";
    }
}