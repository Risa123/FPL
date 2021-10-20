package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public final class Return implements IFunction{
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		var subEnv = (FnSubEnv)env;
		var expCode = "";
		TypeInfo returnType = null;
		if(it.hasNext()){
		    var list = new ArrayList<AExp>();
		    while(it.hasNext()){
		        list.add(it.next());
            }
			var exp = new List(line,tokenNum,list,true);
			var buffer = new BuilderWriter();
		    returnType = exp.compile(buffer,env,it);
			if(!subEnv.getReturnType().equals(returnType)){
				throw new CompilerException(exp,returnType + " cannot be implicitly converted to " + subEnv.getReturnType());
			}
			var code = returnType.ensureCast(subEnv.getReturnType(),buffer.getCode());
			if(subEnv.getToPointerVarID() != 0 || returnType.isPrimitive()){//no destructor calls needed
				expCode = code;
			}else{
				expCode = "tmp";
				writer.write(returnType.getCname() + " tmp=" + code + ";\n");
			}
		}else if(subEnv.getReturnType() != TypeInfo.VOID){
			throw new CompilerException(line,tokenNum,"this function doesn't return void");
		}
		subEnv.compileDestructorCallsFromWholeFunction(writer);
		if(subEnv.isInMainBlock()){
			writer.write("free(args);\n_std_lang_Thread_freeEHEntries0(_std_lang_currentThread);\n");//args is from main module
		}
		if(returnType instanceof InstanceInfo i && i.getCopyConstructorName() != null){//null is always false
           expCode = i.getCopyConstructorName() + "AndReturn(" + expCode + ")";
		}
		writer.write("return " +  expCode);
		return TypeInfo.VOID;
	}
}