package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public final class Return implements IFunction{
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		var subEnv = (FnSubEnv)env;
		if(subEnv.isInMainBlock()){
			writer.write("free(args);\n");//from main block
			writer.write("_std_lang_Thread_freeEHEntries0(_std_lang_currentThread);\n");
		}
		writer.write("return ");
		if(it.hasNext()){
		    var list = new ArrayList<AExp>();
		    while(it.hasNext()){
		        list.add(it.next());
            }
			var exp = new List(line, tokenNum,list,true);
			var buffer = new BuilderWriter(writer);
		    var returnType = exp.compile(buffer,env,it);
			if(!subEnv.getReturnType().equals(returnType)){
				throw new CompilerException(exp,returnType + " cannot be implicitly converted to " + subEnv.getReturnType());
			}
			writer.write(returnType.ensureCast(subEnv.getReturnType(),buffer.getCode()));
		}else if(subEnv.getReturnType() != TypeInfo.VOID){
			throw new CompilerException(line, tokenNum,"this function doesn't return void");
		}
		return TypeInfo.VOID;
	}
}