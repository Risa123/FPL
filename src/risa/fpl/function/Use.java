package risa.fpl.function;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

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
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		var modules = new ArrayList<Atom>();
		if(!(env instanceof ModuleEnv)) {
			throw new CompilerException(line,charNum,"can only be used on module level");
		}
		var exp = it.next();
		if(exp instanceof List) {
			if(it.hasNext()) {
				throw new CompilerException(exp,"only block expected");
			}
			addFromList(exp,modules);
		}else {
		    modules.add((Atom)exp);
			while(it.hasNext()) {
				modules.add(it.nextID());
			}
		}
		for(var mod:modules) {
		    ((ModuleEnv)env).importModule(mod,writer);
		}
		return TypeInfo.VOID;
	}
	private void addFromList(AExp exp,ArrayList<Atom>modules) throws CompilerException {
		for(var mod:((List)exp).getExps()) {
			if(mod instanceof Atom atom) {
				if(atom.getType() != TokenType.ID) {
					throw new CompilerException(atom,"identifier expected");
				}
				if(atom.getValue().equals("std.lang")){
				    throw new CompilerException(atom,"this module is imported automatically");
                }
				modules.add(atom);
			}else {
				addFromList(mod,modules);
			}
		}
	}
	@Override
	public boolean appendSemicolon() {
		return false;
	}
}