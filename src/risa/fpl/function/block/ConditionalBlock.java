package risa.fpl.function.block;

import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.ConditionalBlockEnv;
import risa.fpl.env.SubEnv;
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
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		env.checkModifiers(line,tokenNum);
		builder.append(code).append('(');
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
		var ifEnv = new ConditionalBlockEnv(env);
		var ret = new List(expLine,expCharNum,list,false).compile(builder,ifEnv,it);
		ifEnv.turnOffCompilingCondition();
		if(ret != TypeInfo.BOOL){
			throw new CompilerException(expLine,expCharNum,"expression expected to return bool instead of " + ret);
		}
		builder.append("){\n");
		var block = it.nextList();
		ifEnv.compileBlock(block,builder,it);
		builder.append("}\n");
		if(code.equals("if")){
			if(it.hasNext()){
				var elseExp = it.next();
				if(!(elseExp instanceof List || elseExp instanceof Atom a && a.getValue().equals("if"))){
					throw new CompilerException(elseExp,"else block or if expected");
				}
				if(elseExp instanceof Atom){
					builder.append("else ");
				}else{
					builder.append("else{\n");
				}
				new FnSubEnv(env).compileBlock(elseExp,builder,it);
				if(elseExp instanceof List){
					builder.append("}\n");
				}
			}
		}else if(it.hasNext()){
			throw new CompilerException(block,"no other argument expected");
		}
		return TypeInfo.VOID;
	}
}