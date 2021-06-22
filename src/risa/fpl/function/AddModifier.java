package risa.fpl.function;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ANameSpacedEnv;
import risa.fpl.env.Modifier;
import risa.fpl.function.block.AThreePassBlock;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public class AddModifier extends AThreePassBlock implements IFunction{
   protected final Object mod;
   private boolean appendSemicolon;
   public AddModifier(Object mod){
	   this.mod = mod;
   }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env, ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        addMod(env);
	    try{
	    	var exp = it.next();
	 	    if(exp instanceof List list){
	 	      if(env instanceof ANameSpacedEnv){
                  compile(writer,env,list);
              }else{
	 	          exp.compile(writer,env,it);
              }
	 	    }else{
	 	    	var f = env.getFunction((Atom)exp);
	 	        f.compile(writer,env,it,exp.getLine(),exp.getTokenNum());
	 	        appendSemicolon = f.appendSemicolon();
	 	    }
	    }catch(CompilerException ex){
	        removeMod(env);
	    	throw ex;
	    }
	    removeMod(env);
		return TypeInfo.VOID;
	}
	@Override
	public boolean appendSemicolon(){
		return appendSemicolon;
	}
	public void addMod(AEnv env){
        env.addModifier((Modifier)mod);
    }
    public void removeMod(AEnv env){
       env.removeModifier((Modifier)mod);
    }
}