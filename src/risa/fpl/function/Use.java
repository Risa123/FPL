package risa.fpl.function;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public final class Use implements IFunction{
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		if(!(env instanceof ModuleEnv e)){
			throw new CompilerException(line,tokenNum,"can only be used on module level");
		}
		var exp = it.next();
		if(exp instanceof List){
			if(it.hasNext()){
				throw new CompilerException(exp,"only block expected");
			}
			addFromList(exp,e);
		}else{
		    e.addModuleToImport((Atom)exp);
			while(it.hasNext()){
				e.addModuleToImport(it.nextID());
			}
		}
		return TypeInfo.VOID;
	}
    private void addFromList(AExp exp,ModuleEnv env)throws CompilerException,IOException{
        for(var mod:((List)exp).getExps()){
            if(mod instanceof Atom atom){
                if(atom.getType() != TokenType.ID){
                    throw new CompilerException(atom,"identifier expected");
                }
                if(atom.getValue().equals("std.lang")){
                    throw new CompilerException(atom,"this module is imported automatically");
                }
                env.addModuleToImport(atom);
            }else{
                addFromList(mod,env);
            }
        }
    }
	@Override
	public boolean appendSemicolon(){
		return false;
	}
}