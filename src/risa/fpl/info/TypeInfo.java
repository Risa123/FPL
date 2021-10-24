package risa.fpl.info;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import risa.fpl.env.AEnv;
import risa.fpl.env.IClassOwnedEnv;
import risa.fpl.env.SubEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.exp.*;

public class TypeInfo{
  public static final TypeInfo VOID = new TypeInfo("void","void",true);
  public static final TypeInfo OBJECT = new TypeInfo("object",""){
      @Override
      public void setClassInfo(ClassInfo info){
          addField("getObjectSize",new ValueExp(NumberInfo.MEMORY,null));
          super.setClassInfo(info);
      }
  };
  public static final TypeInfo BOOL = new TypeInfo("bool","char",true);
  public static final TypeInfo CHAR = new TypeInfo("char","char",true);
  public static final TypeInfo NIL = new TypeInfo("nil","");
  static{
	  CHAR.addField("cast",new Cast(CHAR));
	  CHAR.addField("==",new BinaryOperator(BOOL,CHAR,"=="));
	  CHAR.addField("!=",new BinaryOperator(BOOL,CHAR,"!="));
	  BOOL.addField("!",new UnaryOperator(BOOL,"!",false));
	  BOOL.addField("&&",new BinaryOperator(BOOL,BOOL,"&&"));
      BOOL.addField("&",new BinaryOperator(BOOL,BOOL,"&"));
      BOOL.addField("||",new BinaryOperator(BOOL,BOOL,"||"));
      BOOL.addField("|",new BinaryOperator(BOOL,BOOL,"|"));
      BOOL.addField("==",new BinaryOperator(BOOL,BOOL,"=="));
      BOOL.addField("!=",new BinaryOperator(BOOL,BOOL,"!="));
	  NIL.addField("==",new BinaryOperator(BOOL,NIL,"=="));
      NIL.addField("!=",new BinaryOperator(BOOL,NIL,"!="));
      BOOL.addSize();
      CHAR.addSize();
      OBJECT.addField("==",new BinaryOperator(BOOL,OBJECT,"&=="));
      OBJECT.addField("!=",new BinaryOperator(BOOL,OBJECT,"&!="));
  }
  private final String name,cname;
  private String declaration = "";
  private final boolean primitive;
  private final StringBuilder declarationBuilder =  new StringBuilder();
  private final HashMap<String,IField>fields = new HashMap<>();
  private ClassInfo classInfo;
  private final ArrayList<TypeInfo>parents = new ArrayList<>(),requiredTypes = new ArrayList<>();
  private TypeInfo primaryParent;
  public TypeInfo(String name,String cname,boolean primitive){
	  this.name = name;
	  this.cname = cname;
	  this.primitive = primitive;
  }
  public TypeInfo(String name,String cname){
      this(name,cname,false);
  }
  @Override
  public String toString(){
	  return name;
  }
  public final void addField(String name,IField value){
	  fields.put(name,value);
  }
  //returns null if field cannot be accessed from Env from
  public IField getField(String name,AEnv from){
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
          if(from instanceof SubEnv sub && field.getAccessModifier() == AccessModifier.INTERNAL && this instanceof InstanceInfo i && i.getModule() == sub.getModule()){
              return field;
          }else if(from instanceof IClassOwnedEnv e){
              if(field.getAccessModifier() == AccessModifier.PRIVATE && e.getClassType() != null && e.getClassType() == classInfo){
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
      info.setInstanceType(this);
  }
  public final ClassInfo getClassInfo(){
      return classInfo;
  }
  public final String getName(){
      return name;
  }
  public String getCname(){
      return cname;
  }
  public final String getDeclaration(){
      return declaration;
  }
  public void buildDeclaration(){
      for(var field:fields.values()){
        if(field instanceof Variable v){
            addRequiredType(v.getType());
        }else if(field instanceof Function f){
            for(var t:f.getRequiredTypes()){
                addRequiredType(t);
            }
        }
      }
      for(var parent:parents){
          addRequiredType(parent);
      }
      declaration = declarationBuilder.toString();
  }
  protected final void addRequiredType(TypeInfo type){
      if(type != this && !type.isPrimitive() && type.notIn(requiredTypes)){
         requiredTypes.add(type);
      }else if(type instanceof PointerInfo p && p.getType() != this && !p.getType().isPrimitive() && p.getType().notIn(requiredTypes)){
          requiredTypes.add(p.getType());
      }
  }
  public final boolean notIn(Collection<TypeInfo> types){
        for(var t:types){
            if(t == this){
                return false;
            }
        }
        return true;
    }
  public final void appendToDeclaration(String code){
      declarationBuilder.append(code);
  }
  public final boolean isPrimitive(){
      return primitive;
  }
  public final void addParent(TypeInfo parent){
      parents.add(parent);
  }
  public final ArrayList<TypeInfo>getRequiredTypes(){
      return requiredTypes;
  }
  public final ArrayList<TypeInfo>getParents(){
      return parents;
  }
  public final ArrayList<Function>getMethodsOfType(FunctionType type){
      var list = new ArrayList<Function>();
      for(var field:fields.values()){
          if(field instanceof Function f && f.getType() == type){
              list.add(f);
          }
      }
      for(var parent:parents){
          for(var field:parent.getMethodsOfType(type)){
              var found = false;
              for(var f:list){
                  if(f.getName().equals(field.getName())){
                      found = true;
                      break;
                  }
              }
              if(!found){
                  list.add(field);
              }
          }
      }
      return list;
  }
  public boolean notIntegerNumber(){
      return true;
  }
  public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer,boolean notReturnedByFunction){
      if(expCode.endsWith(";\n")){ //caused by Var
          expCode = expCode.substring(0,expCode.length() - 2);
      }
      var npType = to;
      if(npType instanceof PointerInfo p){
          npType = p.getType();
      }
      if(this != npType && this instanceof InstanceInfo instance && npType instanceof InterfaceInfo i && parents.contains(i)){
          var prefix = "";
          var postfix = "";
          if(!comesFromPointer){
            if(notReturnedByFunction){
                prefix = "&";
            }else{
                prefix = instance.getToPointerName() + "(";
                postfix = ")";
            }
          }
          return instance.getConversionMethod(i) + "(" + prefix +  expCode + postfix + ")";
      }
      return expCode;
  }
  public String ensureCast(TypeInfo to,String expCode){
      return ensureCast(to,expCode,false,true);
  }
  public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer){
      return ensureCast(to,expCode,comesFromPointer,true);
  }
    /**
     *checks if type can be implicitly  converted or is  this one
     */
  @Override
  public boolean equals(Object o){
      if(o == TypeInfo.CHAR && this instanceof NumberInfo n && n.getSize() == 1){
          return true;
      }
      if(this == TypeInfo.CHAR && o instanceof NumberInfo n && n.getSize() == 1){
          return true;
      }
      if(o instanceof InterfaceInfo && ((TypeInfo)o).parents.contains(this)){
          return true;
      }
      if(this == OBJECT){
          return true;
      }
      return  this == o;
  }
  public final void setPrimaryParent(TypeInfo primaryParent){
      this.primaryParent = primaryParent;
      addParent(primaryParent);
  }
  public final TypeInfo getPrimaryParent(){
      return primaryParent;
  }
  private void addSize(){
      addField("getObjectSize",new UnaryOperator(NumberInfo.MEMORY,"sizeof ",false));
  }
  public final IField getFieldFromThisType(String name){
      return fields.get(name);
  }
  public final HashMap<String,IField>getFields(){
      return fields;
  }
}