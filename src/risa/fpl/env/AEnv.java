package risa.fpl.env;

import java.util.ArrayList;
import java.util.HashMap;

import risa.fpl.CompilerException;
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
  private final ArrayList<Modifier>mods = new ArrayList<>();
  public IFunction getFunction(Atom atom) throws CompilerException {
	 switch(atom.type) {
	 case ID:
		  if(atom.value.endsWith("*")) {
			   return new Var(getType(atom));
		   }
		 var func = functions.get(atom.value);
		 if(func == null) {
			 throw new CompilerException(atom.line,atom.charNum,"function " + atom.value + " not found" );
		 }
		 return func;
	 case UINT:
		return new ValueExp(NumberInfo.UINT,atom.value);
	 case SINT:
		 return new ValueExp(NumberInfo.SINT,atom.value);
	 case UBYTE:
		 return new ValueExp(NumberInfo.UBYTE,atom.value);
	 case SBYTE:
		 return new ValueExp(NumberInfo.SBYTE,atom.value);
	 case SSHORT:
		 return new ValueExp(NumberInfo.SSHORT,atom.value);
	 case USHORT:
		 return new ValueExp(NumberInfo.USHORT,atom.value);
	 case ULONG:
		 return new ValueExp(NumberInfo.ULONG,atom.value);
	 case SLONG:
		 return new ValueExp(NumberInfo.SLONG,atom.value);
	 case FLOAT:
		 return new ValueExp(NumberInfo.FLOAT,atom.value);
	 case DOUBLE:
		 return new ValueExp(NumberInfo.DOUBLE,atom.value);
	 case CHAR:
		 return new ValueExp(NumberInfo.CHAR,atom.value);
	 case STRING:
		 return new ValueExp(TypeInfo.STRING,atom.value);
	   default:
		   throw new CompilerException(atom.line,atom.charNum,"identifier or literal expected instead of " + atom.type);
	 }
  }
  public boolean hasFunctionInCurrentEnv(String name) {
	  return functions.containsKey(name.replace("*",""));
  }
  public void addFunction(String name,IFunction value) {
	  functions.put(name, value);
  }
  public TypeInfo getType(Atom atom) throws CompilerException {
	  if(atom.type != TokenType.ID) {
		  throw new CompilerException(atom,"type identifier expected");
	  }
	  if(atom.value.endsWith("*")){
	      return new PointerInfo(getType(new Atom(atom.line,atom.charNum,atom.value.substring(0,atom.value.length() - 1), TokenType.ID)));
      }
	  var type = types.get(atom.value);
	  if(type == null){
	      throw new CompilerException(atom,"type " + atom + " not found");
      }
	  return type;
  }
  protected final boolean hasTypeInCurrentEnv(String name){
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
}