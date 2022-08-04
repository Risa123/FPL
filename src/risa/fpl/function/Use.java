package risa.fpl.function;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

public final class Use implements IFunction{
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		env.checkModifiers(line,tokenNum);
		if(!(env instanceof ModuleEnv e)){
			throw new CompilerException(line,tokenNum,"can only be used on module level");
		}
		var exp = it.next();
		if(exp instanceof List){
			if(it.hasNext()){
				error(exp,"only block expected");
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
    private void addFromList(AExp exp,ModuleEnv env)throws CompilerException{
        for(var mod:((List)exp).getExps()){
            if(mod instanceof Atom atom){
                if(atom.getType() != AtomType.ID){
                    error(atom,"identifier expected");
                }
                if(atom.getValue().equals("std.lang")){
                    error(atom,"this module is imported automatically");
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