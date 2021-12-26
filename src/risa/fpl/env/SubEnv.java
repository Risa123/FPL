package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.FPL;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

import java.util.ArrayList;

public class SubEnv extends AEnv{
  protected final AEnv superEnv;
  private int toPointerVarID;
  private AccessModifier accessModifier = AccessModifier.PUBLIC;
  private final ArrayList<Modifier>modifiers = new ArrayList<>();
  private final StringBuilder toPointerVars = new StringBuilder(),destructorCalls = new StringBuilder();
  public SubEnv(AEnv superEnv){
	  this.superEnv = superEnv;
  }
  @Override
  public IFunction getFunction(Atom atom)throws CompilerException{
	if(!hasFunctionInCurrentEnv(atom.getValue())){
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
 @Override
 public final FPL getFPL(){
      return superEnv.getFPL();
 }
 public ModuleEnv getModule(){
      return ((SubEnv)superEnv).getModule();
 }
 public final void addInstanceVariable(InstanceInfo type,String cname){
     var destructor = type.getDestructorName();
     if(destructor != null){//check presence of destructor
         destructorCalls.append(destructor).append("(&").append(cname).append(");\n");
     }
 }
 public final void compileDestructorCalls(StringBuilder builder){
      builder.append(destructorCalls);
 }
 public final String getToPointerVarName(InstanceInfo type){
      var name = "c" + toPointerVarID++;
      toPointerVars.append(type.getCname()).append(' ').append(name).append(";\n");
      var destructor = type.getDestructorName();
      if(destructor != null){
          destructorCalls.append(destructor).append("(&").append(name).append(");\n");
      }
      return name;
 }
 public final void compileToPointerVars(StringBuilder builder){
      builder.append(toPointerVars);
 }
 public final int getToPointerVarID(){
      return toPointerVarID;
 }
 public final void setAccessModifier(AccessModifier accessModifier){
      this.accessModifier = accessModifier;
 }
 public final AccessModifier getAccessModifier(){
      return accessModifier;
 }
 public final boolean hasModifier(Modifier modifier){
      return modifiers.contains(modifier);
 }
 public final void removeModifier(Modifier modifier){
      modifiers.remove(modifier);
 }
 public final void addModifier(Modifier modifier){
      modifiers.add(modifier);
 }
}