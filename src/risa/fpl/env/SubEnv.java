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
    public TypeInfo getType(Atom atom) throws CompilerException {
        if(!hasTypeInCurrentEnv(atom.value)){
            return superEnv.getType(atom);
        }
        return super.getType(atom);
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