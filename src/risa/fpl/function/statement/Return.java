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
		String expCode = null;
		TypeInfo returnType;
		String returnedVariable = null;
		if(it.hasNext()){
			if(subEnv.getReturnType() == TypeInfo.VOID){
				error(line,tokenNum,"no expression expected");
			}
		    var list = new ArrayList<AExp>();
		    while(it.hasNext()){
		        list.add(it.next());
            }
			var exp = new List(line,tokenNum,list,true);
			var buffer = new StringBuilder();
		    returnType = exp.compile(buffer,env,it);
			if(!subEnv.getReturnType().equals(returnType)){
				error(exp,returnType + " cannot be implicitly converted to " + subEnv.getReturnType());
			}
			if(returnType != TypeInfo.VOID){
				var code = returnType.ensureCast(subEnv.getReturnType(),buffer.toString(),env);
				var f = env.getFunction((Atom)list.get(0));
				if((list.size() == 1 && f instanceof ValueExp) || subEnv.hasNoDestructionCallsInThisFunction()){
					expCode = code;
					if(f instanceof Variable v && v.getType() instanceof InstanceInfo i && i.getDestructorName() != null){
						returnedVariable = expCode;
					}
				}else{
					expCode = "ret";
					builder.append(returnType.getCname()).append(" ret=").append(code).append(";\n");
				}
			}
		}else if(subEnv.getReturnType() != TypeInfo.VOID){
			error(line,tokenNum,"this function doesn't return void");
		}
		subEnv.compileDestructorCallsForReturn(builder,returnedVariable);
		if(subEnv.isInMainBlock()){
			builder.append("_std_system_callOnExitHandlers0();\n");//args is from main module
		}
		builder.append("return");
		if(expCode != null){
			builder.append(' ').append(expCode);
		}
		return TypeInfo.VOID;
	}
}