package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.FPL;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SubEnv extends AEnv{
  protected final AEnv superEnv;
  private int toPointerVarID;
  private final ArrayList<Variable>instanceVariables = new ArrayList<>();
  private final StringBuilder toPointerVars = new StringBuilder();
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
 public FPL getFPL(){
      return superEnv.getFPL();
 }
 public ModuleEnv getModule(){
      return ((SubEnv)superEnv).getModule();
 }
 public void addInstanceVariable(Variable instanceVariable){
      instanceVariables.add(instanceVariable);
 }
 public void compileDestructorCalls(BufferedWriter writer)throws IOException{
      var b = new StringBuilder();
      for(var v:instanceVariables){
          var name = ((InstanceInfo)v.getType()).getDestructorName();
          if(name != null){ //null happens when there is no destructor
              b.append(name);
              b.append("(&");
              b.append(v.getCname());
              b.append(");\n");
          }
      }
      writer.write(b.toString());
 }
 public String getToPointerVarName(InstanceInfo type){
      var name = "c" + toPointerVarID++;
      toPointerVars.append(type.getCname());
      toPointerVars.append(' ');
      toPointerVars.append(name);
      toPointerVars.append(";\n");
      return name;
 }
 public void compileToPointerVars(BufferedWriter writer)throws IOException{
      writer.write(toPointerVars.toString());
 }
}