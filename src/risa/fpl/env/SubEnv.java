package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

import java.util.ArrayList;
import java.util.Arrays;

public class SubEnv extends AEnv{
  protected final AEnv superEnv;
  protected int toPointerVarID;
  private AccessModifier accessModifier = AccessModifier.PUBLIC;
  private final ArrayList<Modifier>modifiers = new ArrayList<>();
  protected final StringBuilder toPointerVars = new StringBuilder(),destructorCalls = new StringBuilder();
  private final ArrayList<String>instanceVarCNames = new ArrayList<>();
  public SubEnv(AEnv superEnv){
	  this.superEnv = superEnv;
  }
  @Override
  public IFunction getFunction(Atom atom)throws CompilerException{
	return hasFunctionInCurrentEnv(atom.getValue())?super.getFunction(atom):superEnv.getFunction(atom);
   }
   @Override
   public TypeInfo getType(Atom atom)throws CompilerException{
      return hasTypeInCurrentEnv(atom.getValue())?super.getType(atom):superEnv.getType(atom);
  }
 public ModuleEnv getModule(){
      return ((SubEnv)superEnv).getModule();
 }
 public final void addInstanceVariable(InstanceInfo type,String cname) {
     if(!instanceVarCNames.contains(cname)){//prevent duplicate destructor calls
         var destructor = type.getDestructorName();
         if(destructor != null){
             destructorCalls.append(destructor).append("(&").append(cname).append(");\n");
         }
         instanceVarCNames.add(cname);
     }
 }
 public final String getToPointerVarName(InstanceInfo type){
      var name = addToPointerVar(type);
      var destructor = type.getDestructorName();
      if(destructor != null){
          destructorCalls.append(destructor).append("(&").append(name).append(");\n");
      }
      return name;
 }
 protected String addToPointerVar(InstanceInfo type){
     var name = "c" + toPointerVarID++;
     toPointerVars.append(type.getCname()).append(' ').append(name).append(";\n");
     return name;
 }
 public final void compileDestructorCalls(StringBuilder builder){
      builder.append(destructorCalls);
 }
 public final void compileToPointerVars(StringBuilder builder){
      builder.append(toPointerVars);
 }
 public final boolean hasNoDestructorCalls(){
      return destructorCalls.isEmpty();
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
 public final void checkModifiers(int line,int tokenNum,Modifier...allowed)throws CompilerException{
      for(var mod:modifiers){
          if(!Arrays.asList(allowed).contains(mod)){
              throw new CompilerException(line,tokenNum,"modifier " + mod + " cannot be applied on this function");
          }
      }
 }
}