package risa.fpl.function;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.Modifier;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public final class ModifierBlockStat implements IFunction {
   private final Modifier mod;
   private boolean appendSemicolon;
   public ModifierBlockStat(Modifier mod) {
	   this.mod = mod;
   }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNm)throws IOException, CompilerException {
	    env.addModifier(mod);
	    try {
	    	 var exp = it.next();
	 	    if(exp instanceof List) {
	 	    	exp.compile(writer, env,it);
	 	    }else {
	 	    	var f = env.getFunction((Atom)exp);
	 	        f.compile(writer, env, it, exp.getLine(), exp.getCharNum());
	 	        appendSemicolon = f.appendSemicolon();
	 	    }
	    }catch(CompilerException ex) {
	    	env.removeModifier(mod);
	    	throw ex;
	    }
	    env.removeModifier(mod);
		return TypeInfo.VOID;
	}
	@Override
	public boolean appendSemicolon() {
		return appendSemicolon;
	}
}