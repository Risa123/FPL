package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.ConstructorEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

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
	protected TypeInfo onField(Atom atom,StringBuilder builder,SubEnv env,ExpIterator it,int line,int charNum)throws CompilerException{
		copyCallNeeded = false;
	    var value = atom.getValue();
		if(value.equals("=")){
		   if(constant && !(type instanceof InstanceInfo) && (!(env instanceof ConstructorEnv e) || e.getDefinedConstFields().contains(id))){
			  error(line,charNum,"constant cannot be redefined");
			}
		    var assignmentOperator = false;
		    if(type instanceof InstanceInfo i){
				if(type.getField("=",env) instanceof Function f){
					builder.append(f.getPointerVariant().getCname()).append("(&");
					assignmentOperator = true;
				}
				if(env.hasDestructorCallFor(getCname())){
					env.addInstanceVariable(i,getCname());
				}
			}
			writePrev(builder);
			builder.append(code).append(assignmentOperator?',':'=');
			onlyDeclared = false;
			execute(it,builder,env,"");//not drf equals
			if(assignmentOperator){
				builder.append(')');
			}
			copyCallNeeded = false;
			if(env instanceof ConstructorEnv e && constant){
				e.getDefinedConstFields().add(id);
			}
		    return TypeInfo.VOID;
		}else if(value.equals("ref")){
		    var b = new StringBuilder("&");
            writePrev(b);
		    b.append(code);
		    var ret = new PointerInfo(type);
		    if(it.hasNext() && it.peek() instanceof Atom aID && aID.getType() == AtomType.ID){
		        it.next();
		        var field = ret.getField(aID.getValue(),env);
		        if(field == null){
		            throw new CompilerException(aID,ret + " has no field called " + aID);
                }
		        var code = b.toString();
		        if(field instanceof Function){
		        	code = code.substring(1);
				}
		        field.setPrevCode(code);
		        return field.compile(builder,env,it,aID.getLine(),aID.getTokenNum());
            }
		    builder.append(b);
			return ret;
		}else if(type instanceof PointerInfo){
		    TypeInfo t;
           if((t = processOperator(value,builder,it,env)) != null){
               return t;
           }
        }else if(type instanceof NumberInfo){
			if(constant && value.endsWith("=")){
				error(line,charNum,"constant cannot be redefined");
			}
			if(constant && env instanceof ConstructorEnv e){
				e.getDefinedConstFields().add(id);
			}
			TypeInfo t;
			if((t = processOperator(value,builder,it,env)) != null){
			    return t;
            }
		}
		return super.onField(atom,builder,env,it,line,charNum);
	}
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		if(onlyDeclared && it.hasNext() && it.peek() instanceof Atom a && !(a.getValue().endsWith("=") || a.getValue().equals("ref")) && a.getType() == AtomType.ID){
		    error(line,tokenNum,"variable " + id + " not defined");
        }
		if(instanceType != null && getPrevCode() == null){
		    setPrevCode("((" + instanceType.getCname() + "*)this)->");
        }
		var b = new StringBuilder();
		var ret = super.compile(b,env,it,line,tokenNum);
		if(copyCallNeeded){
			if(type instanceof InstanceInfo i && i.getCopyConstructorName() != null){
				builder.append(i.getCopyConstructorName()).append("AndReturn(");
			}else if(type instanceof InterfaceInfo i){
				builder.append(i.getCopyName()).append("AndReturn(");
			}else{
				copyCallNeeded = false;
			}
		}
		builder.append(b);
		if(copyCallNeeded){
			builder.append(')');
			copyCallNeeded = false;
		}
		return ret;
	}
	private TypeInfo processOperator(String operator,StringBuilder builder,ExpIterator it,SubEnv env)throws CompilerException{
            switch(operator){
                case "+=","-=","/=","*="->{
                    writePrev(builder);
                    process(operator,builder,it,env);
                    return TypeInfo.VOID;
                }
                case "++","--","p+","p-"->{
                    writePrev(builder);
					builder.append(code);
                    if(operator.equals("p+")){
                        operator = "++";
                    }else if(operator.equals("p-")){
                        operator = "--";
                    }
                    builder.append(operator);
                    return type;
                }
            }
            if(operator.equals("%=") && (type instanceof NumberInfo n && !n.isFloatingPoint() || type instanceof PointerInfo)){
                process(operator,builder,it,env);
                return TypeInfo.VOID;
            }else if(type instanceof PointerInfo p && operator.equals("drf=")){
                if(p.getType() instanceof InstanceInfo i && i.getCopyConstructorName() != null){
                	builder.append(i.getCopyConstructorName()).append('(');
					writePrev(builder);
					builder.append(code).append(",&");
					var exp = it.nextAtom();
					var func = env.getFunction(exp);
					if(func instanceof Variable v){
						v.copyCallNeeded = false;
					}
					var ret  = func.compile(builder,env,it,exp.getLine(),exp.getTokenNum());
					if(!i.equals(ret)){
						error(exp,"expression expected to return " + i + " instead of " + ret);
					}
					builder.append(");\n");
				}else{
					builder.append('*');
					writePrev(builder);
					process("=",builder,it,env);
				}
                return TypeInfo.VOID;
            }
	    return null;
    }
    private void process(String operator,StringBuilder builder,ExpIterator it,SubEnv env)throws CompilerException{
	    builder.append(code).append(operator);
        execute(it,builder,env,operator);
    }
    private void execute(ExpIterator it,StringBuilder builder,SubEnv env,String operator)throws CompilerException{
	    var exp = it.next();
	    var ret = exp.compile(builder,env,it);
	    var t = operator.equals("=") && type instanceof PointerInfo p?p.getType():type;
	    if(!t.equals(ret)){
	        error(exp,"expression expected to return  " + t + " instead of " + ret);
        }
    }
    public TypeInfo getType(){
	    return type;
    }
    public String getExternDeclaration(){
	    return getAccessModifier() == AccessModifier.PRIVATE?"":"extern " + type.getCname() + ' ' + code + ";\n";
    }
    public String getCname(){
		return code;
	}
	public boolean isConstant(){
		return constant;
	}
	public String getId(){
		return id;
	}
}