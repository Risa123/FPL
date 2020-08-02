package risa.fpl.info;

import java.util.HashMap;

import risa.fpl.env.AEnv;
import risa.fpl.env.IClassOwnedEnv;
import risa.fpl.function.AccessModifier;
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
  private final String name,cname;
  private String declaration = "";
  private final StringBuilder declarationBuilder =  new StringBuilder();
  private final HashMap<String, IField>fields = new HashMap<>();
  private Function constructor;
  private ClassInfo classInfo;
  public TypeInfo(String name,String cname) {
	  this.name = name;
	  this.cname = cname;
  }
  @Override
  public String toString() {
	  return name;
  }
  public void addField(String name, IField value) {
	  fields.put(name, value);
  }
  //denies access if fields is private and field is not requested by its class
  public IField getField(String name,AEnv from) {
      var field = fields.get(name);
      if(field != null && field.getAccessModifier() != AccessModifier.PUBLIC){
          if(from instanceof IClassOwnedEnv e && e.getClassType() == classInfo){
              return field;
          }
          return null;
      }
	  return field;
  }
  public void setClassInfo(ClassInfo info){
      classInfo = info;
      classInfo.addField("size",new ValueExp(NumberInfo.MEMORY,"sizeof " + cname));
  }
  public ClassInfo getClassInfo(){
      return classInfo;
  }
  public final String getName(){
      return name;
  }
  public final String getCname(){
      return cname;
  }
  public final String getDeclaration(){
      return declaration;
  }
  public final void buildDeclaration(){
      declaration = declarationBuilder.toString();
  }
  public final void appendToDeclaration(String code){
      declarationBuilder.append(code);
  }
  public final void appendToDeclaration(char c){
      declarationBuilder.append(c);
  }
  public final Function getConstructor(){
      return constructor;
  }
  public final void setConstructor(Function constructor){
      this.constructor = constructor;
  }
}