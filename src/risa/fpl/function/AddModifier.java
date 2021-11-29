package risa.fpl.function;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.ANameSpacedEnv;
import risa.fpl.env.Modifier;
import risa.fpl.env.SubEnv;
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
	public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        addModifier(env);
	    try{
	    	var exp = it.next();
	 	    if(exp instanceof List list){
	 	      if(env instanceof ANameSpacedEnv e){
                 var infos = e.getModifierBlock(line);
				 if(infos == null){
					 infos = createInfoList(list);
					 e.addModifierBlock(line,infos);
				 }
				 compile(writer,env,infos);
              }else{
	 	          exp.compile(writer,env,it);
              }
	 	    }else{
	 	    	var f = env.getFunction((Atom)exp);
	 	        f.compile(writer,env,it,exp.getLine(),exp.getTokenNum());
	 	        appendSemicolon = f.appendSemicolon();
	 	    }
	    }catch(CompilerException ex){
	        removeModifier(env);
	    	throw ex;
	    }
	    removeModifier(env);
		return TypeInfo.VOID;
	}
	@Override
	public boolean appendSemicolon(){
		return appendSemicolon;
	}
	public void addModifier(SubEnv env){
        env.addModifier((Modifier)mod);
    }
    public void removeModifier(SubEnv env){
       env.removeModifier((Modifier)mod);
    }
}