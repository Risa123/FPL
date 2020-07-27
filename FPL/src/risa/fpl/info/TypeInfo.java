package risa.fpl.info;

import java.util.HashMap;

import risa.fpl.function.exp.AField;
import risa.fpl.function.exp.AsByte;
import risa.fpl.function.exp.GetIndex;
import risa.fpl.function.exp.UnaryOperator;

public class TypeInfo {
  public static final TypeInfo VOID = new TypeInfo("void","void");
  public static final TypeInfo OBJECT = new TypeInfo("object","");
  public static final TypeInfo BOOL = new TypeInfo("bool","char");
  public static final TypeInfo STRING = new TypeInfo("string","char*");
  public static final TypeInfo CHAR = new TypeInfo("char","char");
  public static final TypeInfo NULL = new TypeInfo("null","");
  static {
	  CHAR.addField("asByte",new AsByte());
	  STRING.addField("get",new GetIndex(CHAR));
	  BOOL.addField("!",new UnaryOperator(BOOL,"!",false));
  }
  public final String name,cname,declaration;
  private final HashMap<String,AField>fields = new HashMap<>();
  public TypeInfo(String name,String cname,String declaration) {
	  this.name = name;
	  this.cname = cname;
	  this.declaration = declaration;
  }
  public TypeInfo(String name,String cname) {
	  this(name,cname,"");
  }
  @Override
  public String toString() {
	  return name;
  }
  public void addField(String name,AField value) {
	  fields.put(name, value);
  }
  public AField getField(String name) {
	  return fields.get(name);
  }
}