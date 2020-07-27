package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

public class SubEnv extends AEnv {
  private final AEnv superEnv;
  private boolean exitStatement;
  public SubEnv(AEnv superEnv) {
	  this.superEnv = superEnv;
  }
@Override
public IFunction getFunction(Atom atom) throws CompilerException {
	if(!hasFunctionInCurrentEnv(atom.value)) {
		return superEnv.getFunction(atom);
	}
	return super.getFunction(atom);
}
@Override
public TypeInfo getTypeUnsafe(String name) {
	var type = super.getTypeUnsafe(name);
	if(type == null) {
		type = superEnv.getTypeUnsafe(name);
	}
	return type;
 }
 public TypeInfo getReturnType() {
	 return ((SubEnv)superEnv).getReturnType();
 }
 public boolean containsExitStatement() {
	 return exitStatement;
 }
 public void exitStatement() {
	 exitStatement = true;
 }
}