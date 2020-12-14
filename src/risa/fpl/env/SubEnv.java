package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.FPL;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

public class SubEnv extends AEnv {
  protected final AEnv superEnv;
  public SubEnv(AEnv superEnv) {
	  this.superEnv = superEnv;
  }
  @Override
  public IFunction getFunction(Atom atom)throws CompilerException{
	if(!hasFunctionInCurrentEnv(atom.getValue())) {
		return superEnv.getFunction(atom);
	}
	return super.getFunction(atom);
   }
   @Override
   public TypeInfo getType(Atom atom)throws CompilerException{
      if(!hasTypeInCurrentEnv(atom.getValue())){
          return superEnv.getType(atom);
      }
      return super.getType(atom);
  }
  public TypeInfo getReturnType(){
      return ((SubEnv)superEnv).getReturnType();
 }
 @Override
 public FPL getFPL(){
      return superEnv.getFPL();
 }
 public ModuleEnv getModule(){
      return ((SubEnv)superEnv).getModule();
 }
}