package risa.fpl.function;

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
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        addModifier(env,line,tokenNum);
	    try{
	    	var exp = it.next();
	 	    if(exp instanceof List list){
	 	      if(env instanceof ANameSpacedEnv e){
                 var infos = e.getModifierBlockInfos(line);
				 if(infos == null){
					 infos = createInfoList(list);
					 e.addModifierBlockInfos(line,infos);
				 }
				 compile(builder,env,infos);
              }else{
	 	          exp.compile(builder,env,it);
              }
	 	    }else{
	 	    	var f = env.getFunction((Atom)exp);
	 	        f.compile(builder,env,it,exp.getLine(),exp.getTokenNum());
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
	protected void addModifier(SubEnv env,int line,int tokenNum)throws CompilerException{
	    if(env.hasModifier((Modifier)mod)){
			throw new CompilerException(line,tokenNum,"duplicate modifier " + mod.toString().toLowerCase());
		}
        env.addModifier((Modifier)mod);
    }
    protected void removeModifier(SubEnv env){
       env.removeModifier((Modifier)mod);
    }
}