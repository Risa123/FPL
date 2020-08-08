package risa.fpl.info;

import java.util.ArrayList;
import java.util.HashMap;

import risa.fpl.env.AEnv;
import risa.fpl.env.IClassOwnedEnv;
import risa.fpl.env.Modifier;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.exp.*;

public class TypeInfo {
  public static final TypeInfo VOID = new TypeInfo("void","void",true);
  public static final TypeInfo OBJECT = new TypeInfo("object","");
  public static final TypeInfo BOOL = new TypeInfo("bool","char",true);
  public static final TypeInfo STRING = new TypeInfo("string","char*",true);
  public static final TypeInfo CHAR = new TypeInfo("char","char",true);
  public static final TypeInfo NIL = new TypeInfo("nil","");
  static {
	  CHAR.addField("cast",new Cast(CHAR));
	  STRING.addField("get",new GetIndex(CHAR));
	  BOOL.addField("!",new UnaryOperator(BOOL,"!",false));
	  NIL.addField("==",new BinaryOperator(BOOL, NIL,"=="));
      NIL.addField("!=",new BinaryOperator(BOOL, NIL,"!="));
  }
  private final String name,cname;
  private String declaration = "";
  private final boolean primitive;
  private final StringBuilder declarationBuilder =  new StringBuilder();
  private final HashMap<String, IField>fields = new HashMap<>();
  private Function constructor;
  private ClassInfo classInfo;
  private final ArrayList<TypeInfo>parents = new ArrayList<>();
  private final ArrayList<TypeInfo>requiredTypes = new ArrayList<>();
  public TypeInfo(String name,String cname,boolean primitive) {
	  this.name = name;
	  this.cname = cname;
	  this.primitive = primitive;
  }
  public TypeInfo(String name,String cname){
      this(name,cname,false);
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
      if(field == null){
          for(var parent:parents){
              field = parent.getField(name,from);
              if(field  != null){
                  break;
              }
          }
      }
      if(field != null && field.getAccessModifier() != AccessModifier.PUBLIC){
          if(from instanceof IClassOwnedEnv e){
              if(field.getAccessModifier() == AccessModifier.PRIVATE && e.getClassType() == classInfo){
                  return field;
              }else if(field.getAccessModifier() == AccessModifier.PROTECTED){
                  return field;
              }
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
  public final void buildDeclaration(AEnv env){
      for(var field:fields.values()){
        if(field instanceof Variable v){
            addType(v.getType(),env);
        }else if(field instanceof Function f){
            addType(f.getReturnType(),env);
           for(var arg:f.getArguments()){
               addType(arg,env);
           }
        }
      }
      declaration = declarationBuilder.toString();
  }
  private void addType(TypeInfo type, AEnv env){
      if(!env.hasTypeInCurrentEnv(type.getName())&& !type.isPrimitive() && !requiredTypes.contains(type)){
         requiredTypes.add(type);
      }
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
  public final boolean isPrimitive(){
      return primitive;
  }
  public void addParent(TypeInfo parent){
      parents.add(parent);
  }
  public boolean containsAllParents(ArrayList<TypeInfo>types){
      return types.containsAll(parents);
  }
  public ArrayList<TypeInfo>getRequiredTypes(){
      return requiredTypes;
  }
  public ArrayList<TypeInfo>getParents(){
      return parents;
  }
  public ArrayList<Function>getAbstractMethods(){
      var list = new ArrayList<Function>();
      for(var field:fields.values()){
          if(field instanceof Function f && f.getType() == Modifier.ABSTRACT){
              list.add(f);
          }
      }
      for(var parent:parents){
          list.addAll(parent.getAbstractMethods());
      }
      return list;
  }
  public boolean isIntegerNumber(){
      if(this instanceof NumberInfo n){
          return !n.isFloatingPoint();
      }
      return false;
  }
  public boolean hasFieldIgnoreParents(String name){
      return fields.containsKey(name);
  }
}