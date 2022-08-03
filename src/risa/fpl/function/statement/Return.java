package risa.fpl.function.statement;

import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.*;

public final class Return implements IFunction{
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		env.checkModifiers(line,tokenNum);
		var subEnv = (FnSubEnv)env;
		var expCode = "";
		TypeInfo returnType;
		if(it.hasNext()){
			if(subEnv.getReturnType() == TypeInfo.VOID){
				throw new CompilerException(line,tokenNum,"no expression expected");
			}
		    var list = new ArrayList<AExp>();
		    while(it.hasNext()){
		        list.add(it.next());
            }
			var exp = new List(line,tokenNum,list,true);
			var buffer = new StringBuilder();
		    returnType = exp.compile(buffer,env,it);
			if(!subEnv.getReturnType().equals(returnType)){
				throw new CompilerException(exp,returnType + " cannot be implicitly converted to " + subEnv.getReturnType());
			}
			if(returnType != TypeInfo.VOID){
				var code = returnType.ensureCast(subEnv.getReturnType(),buffer.toString(),env);
				if(subEnv.hasNoDestructorCalls() || (list.size() == 1 && env.getFunction((Atom)list.get(0)) instanceof ValueExp v && !(v instanceof  Variable))){
					expCode = code;
				}else{
					expCode = "tmp";
					builder.append(returnType.getCname()).append(" tmp=");
					if(returnType instanceof InstanceInfo i && i.getCopyConstructorName() != null && ((Atom)list.get(0)).getType() != AtomType.STRING){
						code = i.getCopyConstructorName() + "AndReturn("+ code + ')';
					}
					builder.append(code).append(";\n");
				}
			}
			//code of instance variable already starts with copyAndReturn
			if(!expCode.equals("tmp") && returnType instanceof InstanceInfo i && i.getCopyConstructorName() != null && !expCode.startsWith(i.getCopyConstructorName())){
				expCode = i.getCopyConstructorName() + "AndReturn(" + expCode + ')';
			}
		}else if(subEnv.getReturnType() != TypeInfo.VOID){
			throw new CompilerException(line,tokenNum,"this function doesn't return void");
		}
		subEnv.compileDestructorCallsFromWholeFunction(builder);
		if(subEnv.isInMainBlock()){
			builder.append("_std_system_callOnExitHandlers0();\n");//args is from main module
		}
		builder.append("return");
		if(!expCode.isEmpty()){
			builder.append(' ').append(expCode);
		}
		return TypeInfo.VOID;
	}
}