package risa.fpl.env;

import java.util.HashMap;

import risa.fpl.CompilerException;
import risa.fpl.FPL;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.function.statement.Var;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.AtomType;

public abstract class AEnv{
  protected final HashMap<String,TypeInfo>types = new HashMap<>();
  protected final HashMap<String,IFunction>functions = new HashMap<>();
  public IFunction getFunction(Atom atom)throws CompilerException{
	 switch(atom.getType()){
	  case ID:
		 if(atom.getValue().endsWith("*")){
			 return new Var(getType(atom));
		 }
		 var func = functions.get(atom.getValue());
		 if(func == null){
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
		 return new ValueExp(FPL.getString(),"static_std_lang_String_new0(" + atom.getValue() + "," + (atom.getValue().length() - 2) + ",0)");
	   default:
		   throw new CompilerException(atom,"identifier or literal expected instead of " + atom.getType());
	 }
  }
  public boolean hasFunctionInCurrentEnv(String name){
	  return functions.containsKey(name.replace("*",""));
  }
  public void addFunction(String name,IFunction value){
	  functions.put(name,value);
  }
  public TypeInfo getType(Atom atom)throws CompilerException{
	  if(atom.getType() != AtomType.ID){
		  throw new CompilerException(atom,"type identifier expected");
	  }
	  if(atom.getValue().endsWith("*")){
	      return new PointerInfo(getType(new Atom(atom.getLine(),atom.getTokenNum(),atom.getValue().substring(0,atom.getValue().length() - 1), AtomType.ID)));
      }
	  var type = types.get(atom.getValue());
	  if(type == null){
	      throw new CompilerException(atom,"type " + atom + " not found");
      }
	  return type;
  }
  public final boolean hasTypeInCurrentEnv(String name){
      return types.containsKey(name.replace("*",""));//remove pointer's * from name
  }
  public final void addType(TypeInfo type){
	  addType(type,true);
  }
  public void addType(TypeInfo type,boolean declaration){
      types.put(type.getName(),type);
      if(declaration){
		  addFunction(type.getName(),type instanceof InstanceInfo i?i.getConstructor():new Var(type));
      }
  }
}