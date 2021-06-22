package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public final class ConditionalBlock extends ABlock{
    private final String code;
    public ConditionalBlock(String code){
    	this.code = code;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
		writer.write(code);
		writer.write('(');
		var list = new ArrayList<AExp>();
		int expLine = 0,expCharNum = 0;
		while(it.hasNext()){
			var exp = it.peek();
			if(expLine == 0){
				expLine = exp.getLine();
				expCharNum = exp.getTokenNum();
			}
			if(exp instanceof List){
				break;
			}else{
				it.next();
				list.add(exp);
			}
		}
		var ret = new List(expLine,expCharNum,list,false).compile(writer,new FnSubEnv(env),it);
		if(ret != TypeInfo.BOOL){
			throw new CompilerException(expLine,expCharNum,"expression expected to return bool instead of " + ret);
		}
		writer.write("){\n");
		var ifEnv = new FnSubEnv(env);
		var block = it.nextList();
		block.compile(writer,ifEnv,it);
		ifEnv.compileDestructorCalls(writer);
		writer.write("}\n");
		if(code.equals("if")){
			if(it.hasNext()){
				var elseExp = it.next();
				if(!(elseExp instanceof List || elseExp instanceof Atom a && a.getValue().equals("if"))){
					throw new CompilerException(elseExp,"else block or if expected");
				}
				if(elseExp instanceof Atom){
					writer.write("else ");
				}else{
					writer.write("else{\n");
				}
				var subEnv = new FnSubEnv(env);
				elseExp.compile(writer,subEnv,it);
				subEnv.compileDestructorCalls(writer);
				if(elseExp instanceof List){
					writer.write("}\n");
				}
			}
		}else if(it.hasNext()){
			throw new CompilerException(block,"no other argument expected");
		}
		return TypeInfo.VOID;
	}
}