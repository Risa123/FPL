package risa.fpl.info;

import java.util.HashMap;

import risa.fpl.function.exp.*;

public class TypeInfo {
  public static final TypeInfo VOID = new TypeInfo("void","void");
  public static final TypeInfo OBJECT = new TypeInfo("object","");
  public static final TypeInfo BOOL = new TypeInfo("bool","char");
  public static final TypeInfo STRING = new TypeInfo("string","char*");
  public static final TypeInfo CHAR = new TypeInfo("char","char");
  public static final TypeInfo NIL = new TypeInfo("nil","");
  static {
	  CHAR.addField("asByte",new AsByte());
	  STRING.addField("get",new GetIndex(CHAR));
	  BOOL.addField("!",new UnaryOperator(BOOL,"!",false));
	  NIL.addField("==",new BinaryOperator(BOOL, NIL,"=="));
      NIL.addField("!=",new BinaryOperator(BOOL, NIL,"!="));
  }
  public final String name,cname,declaration;
  private final HashMap<String, IField>fields = new HashMap<>();
  public final Function constructor;
  private ClassInfo classInfo;
  public TypeInfo(String name,String cname,String declaration,Function constructor) {
	  this.name = name;
	  this.cname = cname;
	  this.declaration = declaration;
	  this.constructor = constructor;
  }
  public TypeInfo(String name,String cname) {
	  this(name,cname,"",null);
  }
  @Override
  public String toString() {
	  return name;
  }
  public void addField(String name, IField value) {
	  fields.put(name, value);
  }
  public IField getField(String name) {
	  return fields.get(name);
  }
  public void setClassInfo(ClassInfo info){
      classInfo = info;
      classInfo.addField("size",new ValueExp(NumberInfo.MEMORY,"sizeof " + cname));
  }
  public ClassInfo getClassInfo(){
      return classInfo;
  }
}