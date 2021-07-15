package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class Variable extends ValueExp{
	private boolean onlyDeclared,copyCallNeeded = true;
	private final String id;
	private final boolean constant;
	private final TypeInfo instanceType;
	public Variable(TypeInfo type,String code,boolean onlyDeclared,String id,boolean constant,TypeInfo instanceType,AccessModifier mod){
		super(type,code,mod);
		this.onlyDeclared = onlyDeclared;
		this.id = id;
		this.constant = constant;
		this.instanceType = instanceType;
	}
    public Variable(TypeInfo type,String code,String id){
       this(type,code,false,id,false,null,AccessModifier.PUBLIC);
    }
	@Override
	protected TypeInfo onField(Atom atom,BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws CompilerException,IOException{
		copyCallNeeded = false;
	    var value = atom.getValue();
		if(value.equals("=")){
		   if(constant && !(type instanceof InstanceInfo)){
			  throw new CompilerException(line,charNum,"constant cannot be redefined");
			}
		    var assignmentOperator = false;
		    if(type instanceof InstanceInfo && type.getField("=",env) instanceof Function f){
		    	writer.write(f.getPointerVariant().cname() + "(&");
		    	assignmentOperator = true;
			}
			writePrev(writer);
			writer.write(code);
			if(assignmentOperator){
				writer.write(',');
			}else{
				writer.write('=');
			}
			onlyDeclared = false;
			execute(it,writer,env,"");//not drf equals
			if(assignmentOperator){
				writer.write(')');
			}
			copyCallNeeded = false;
		    return TypeInfo.VOID;
		}else if(value.equals("ref")){
		    var b = new BuilderWriter(writer);
            b.write('&');
            writePrev(b);
		    b.write(code);
		    var ret = new PointerInfo(type);
		    if(it.hasNext() && it.peek() instanceof Atom id && id.getType() == TokenType.ID){
		        it.next();
		        var field = ret.getField(id.getValue(),env);
		        if(field == null){
		            throw new CompilerException(id,ret + " has no field called " + id);
                }
		        var code = b.getCode();
		        if(field instanceof Function){
		        	code = code.substring(1);
				}
		        field.setPrevCode(code);
		        return field.compile(writer,env,it,id.getLine(),id.getTokenNum());
            }
		    writer.write(b.getCode());
			return ret;
		}else if(type instanceof PointerInfo){
		    TypeInfo t;
           if((t = processOperator(value,writer,it,env)) != null){
               return t;
           }
        }else if(type instanceof NumberInfo){
			if(constant && value.endsWith("=")){
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
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		if(onlyDeclared && it.hasNext() && it.peek() instanceof Atom a && !a.getValue().endsWith("=") && a.getType() == TokenType.ID){
		    throw new CompilerException(line, tokenNum,"variable " + id + " not defined");
        }
		if(instanceType != null && getPrevCode() == null){
		    setPrevCode("((" + instanceType.getCname() + "*)this)->");
        }
		var b = new BuilderWriter(writer);
		var ret = super.compile(b,env,it,line,tokenNum);
		copyCallNeeded = copyCallNeeded && type instanceof InstanceInfo i && i.getCopyConstructorName() != null;
		if(copyCallNeeded){
			writer.write(((InstanceInfo)type).getCopyConstructorName() + "AndReturn(");
		}
		writer.write(b.getCode());
		if(copyCallNeeded){
			writer.write(')');
		}
		copyCallNeeded = true;
		return ret;
	}
	private TypeInfo processOperator(String operator,BufferedWriter writer,ExpIterator it,AEnv env) throws IOException,CompilerException{
            switch(operator){
                case "+=","-=","/=","*=" ->{
                    writePrev(writer);
                    process(operator,writer,it,env);
                    return TypeInfo.VOID;
                }
                case "++","--","p+","p-" ->{
                    writePrev(writer);
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
            }else if(type instanceof PointerInfo p && operator.equals("drf=")){
                if(p.getType() instanceof InstanceInfo i && i.getCopyConstructorName() != null){
                	writer.write(i.getCopyConstructorName() + "(");
					writePrev(writer);
					writer.write(code + ",&");
					var exp = it.nextAtom();
					var func = env.getFunction(exp);
					if(func instanceof Variable v){
						v.copyCallNeeded = false;
					}
					var ret  = func.compile(writer,env,it,exp.getLine(),exp.getTokenNum());
					if(!i.equals(ret)){
						throw new CompilerException(exp,"expression expected to return " + i + " instead of " + ret);
					}
					writer.write(");\n");
				}else{
					writer.write('*');
					writePrev(writer);
					process("=",writer,it,env);
				}
                return TypeInfo.VOID;
            }
	    return null;
    }
    private void process(String operator,BufferedWriter writer,ExpIterator it,AEnv env)throws IOException,CompilerException{
	    writer.write(code);
        writer.write(operator);
        execute(it,writer,env,operator);
    }
    private void execute(ExpIterator it,BufferedWriter writer,AEnv env,String operator)throws CompilerException,IOException{
	    var exp = it.next();
	    var ret = exp.compile(writer,env,it);
	    var t = type;
	    if(operator.equals("=") && type instanceof PointerInfo p){
	        t = p.getType();
        }
	    if(!t.equals(ret)){
	        throw new CompilerException(exp,"expression expected to return  " + t + " instead of " + ret);
        }
    }
    public TypeInfo getType(){
	    return type;
    }
    public String getExternDeclaration(){
		if(getAccessModifier() == AccessModifier.PRIVATE){
			return "";
		}
	    return "extern " + type.getCname() + " " + code + ";\n";
    }
    public String getCname(){
		return code;
	}
}