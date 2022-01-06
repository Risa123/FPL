package risa.fpl.function.statement;

import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public final class Return implements IFunction{
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		env.checkModifiers(line,tokenNum);
		var subEnv = (FnSubEnv)env;
		var expCode = "";
		TypeInfo returnType = null;
		if(it.hasNext()){
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
				var code = returnType.ensureCast(subEnv.getReturnType(),buffer.toString());
				if(subEnv.getToPointerVarID() == 0){//no destructor calls needed
					expCode = code;
				}else{
					expCode = "tmp";
					builder.append(returnType.getCname()).append(" tmp=").append(code).append(";\n");
				}
			}
		}else if(subEnv.getReturnType() != TypeInfo.VOID){
			throw new CompilerException(line,tokenNum,"this function doesn't return void");
		}
		subEnv.compileDestructorCallsFromWholeFunction(builder);
		if(subEnv.isInMainBlock()){
			builder.append("void _std_system_callOnExitHandlers();\n");
			builder.append("_std_system_callOnExitHandlers0();\n");//args is from main module
		}
		if(returnType instanceof InstanceInfo i && i.getCopyConstructorName() != null){//null is always false
           expCode = i.getCopyConstructorName() + "AndReturn(" + expCode + ")";
		}
		builder.append("return ").append(expCode);
		return TypeInfo.VOID;
	}
}