package risa.fpl.env;

import java.util.ArrayList;
import java.util.HashMap;

import risa.fpl.CompilerException;
import risa.fpl.FPL;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.function.statement.Var;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.tokenizer.TokenType;

public abstract class AEnv {
  protected final HashMap<String,TypeInfo>types = new HashMap<>();
  protected final HashMap<String,IFunction>functions = new HashMap<>();
  protected AccessModifier accessModifier = AccessModifier.PUBLIC;
  private final ArrayList<Modifier>mods = new ArrayList<>();
  public IFunction getFunction(Atom atom) throws CompilerException {
	 switch(atom.getType()) {
	 case ID:
		  if(atom.getValue().endsWith("*")) {
			   return new Var(getType(atom));
		   }
		 var func = functions.get(atom.getValue());
		 if(func == null) {
			 throw new CompilerException(atom,"function " + atom + " not found" );
		 }
		 return func;
	 case UINT:
		return new ValueExp(NumberInfo.UINT,atom.getValue());
	 case SINT:
		 return new ValueExp(NumberInfo.SINT,atom.getValue());
	 case UBYTE:
		 return new ValueExp(NumberInfo.UBYTE,atom.getValue());
	 case SBYTE:
		 return new ValueExp(NumberInfo.SBYTE,atom.getValue());
	 case SSHORT:
		 return new ValueExp(NumberInfo.SSHORT,atom.getValue());
	 case USHORT:
		 return new ValueExp(NumberInfo.USHORT,atom.getValue());
	 case ULONG:
		 return new ValueExp(NumberInfo.ULONG,atom.getValue());
	 case SLONG:
		 return new ValueExp(NumberInfo.SLONG,atom.getValue());
	 case FLOAT:
		 return new ValueExp(NumberInfo.FLOAT,atom.getValue());
	 case DOUBLE:
		 return new ValueExp(NumberInfo.DOUBLE,atom.getValue());
	 case CHAR:
		 return new ValueExp(NumberInfo.CHAR,atom.getValue());
	 case STRING:
		 return new ValueExp(TypeInfo.STRING,atom.getValue());
	   default:
		   throw new CompilerException(atom.getLine(),atom.getCharNum(),"identifier or literal expected instead of " + atom.getType());
	 }
  }
  public boolean hasFunctionInCurrentEnv(String name) {
	  return functions.containsKey(name.replace("*",""));
  }
  public void addFunction(String name,IFunction value) {
	  functions.put(name, value);
  }
  public TypeInfo getType(Atom atom) throws CompilerException {
	  if(atom.getType() != TokenType.ID) {
		  throw new CompilerException(atom,"type identifier expected");
	  }
	  if(atom.getValue().endsWith("*")){
	      return new PointerInfo(getType(new Atom(atom.getLine(),atom.getCharNum(),atom.getValue().substring(0,atom.getValue().length() - 1), TokenType.ID)));
      }
	  var type = types.get(atom.getValue());
	  if(type == null){
	      throw new CompilerException(atom,"type " + atom + " not found");
      }
	  return type;
  }
  public final boolean hasTypeInCurrentEnv(String name){
      return types.containsKey(name.replace("*","")); //remove pointer declarations
  }
  public void addType(String name,TypeInfo type) {
	  addType(name,type,true);
  }
  public void addType(String name,TypeInfo type,boolean declaration){
      types.put(name,type);
      if(declaration){
          addFunction(name,new Var(type));
      }
  }
  public boolean hasModifier(Modifier mod) {
	 return mods.contains(mod);
  }
  public void addModifier(Modifier mod) {
	  mods.add(mod);
  }
  public void removeModifier(Modifier mod) {
	  mods.remove(mod);
  }
  public final void setAccessModifier(AccessModifier mod){
      accessModifier = mod;
  }
  public final AccessModifier getAccessModifier(){
      return accessModifier;
  }
  public abstract FPL getFPL();
}