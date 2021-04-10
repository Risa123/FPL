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
  public static final TypeInfo STRING = new TypeInfo("string","char*",true);
  public static final TypeInfo CHAR = new TypeInfo("char","char",true);
  public static final TypeInfo NIL = new TypeInfo("nil","");
  static{
	  CHAR.addField("cast",new Cast(CHAR));
	  CHAR.addField("==",new BinaryOperator(BOOL,CHAR,"=="));
	  CHAR.addField("!=",new BinaryOperator(BOOL,CHAR,"!="));
	  STRING.addField("get",new GetElement(CHAR));
	  STRING.addField("set",new SetElement(CHAR));
	  STRING.addField("cast",new Cast(STRING));
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
      STRING.addSize();
  }
  private final String name,cname;
  private String declaration = "";
  private final boolean primitive;
  private final StringBuilder declarationBuilder =  new StringBuilder();
  private final HashMap<String, IField>fields = new HashMap<>();
  private Function constructor;
  private ClassInfo classInfo;
  private final ArrayList<TypeInfo>parents = new ArrayList<>(),requiredTypes = new ArrayList<>();
  private final HashMap<TypeInfo,String> conversionMethodCNames = new HashMap<>();
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
  public void addField(String name,IField value){
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
              if(field.getAccessModifier() == AccessModifier.PRIVATE && e.getClassType() != null && e.getClassType().getName().equals(classInfo.getName())){
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
  }
  public ClassInfo getClassInfo(){
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
            addRequiredType(f.getReturnType());
           for(var arg:f.getArguments()){
               addRequiredType(arg);
           }
        }
      }
      for(var parent:parents){
          addRequiredType(parent);
      }
      declaration = declarationBuilder.toString();
  }
  protected void addRequiredType(TypeInfo type){
      if(!type.isPrimitive() && type.notIn(requiredTypes)){
         requiredTypes.add(type);
      }else if(type instanceof PointerInfo p && !p.getType().isPrimitive() && p.isArray()){
          requiredTypes.add(p.getType());
      }
  }
  public boolean notIn(Collection<TypeInfo> types){
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
  public ArrayList<TypeInfo>getRequiredTypes(){
      return requiredTypes;
  }
  public ArrayList<TypeInfo>getParents(){
      return parents;
  }
  public ArrayList<Function> getMethodsOfType(FunctionType type){
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
      if(this instanceof NumberInfo n){
          return n.isFloatingPoint();
      }
      return true;
  }
  public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer){
      if(expCode.endsWith(";\n")){ //caused by Var
          expCode = expCode.substring(0,expCode.length() - 2);
      }
      var npType = to;
      if(npType instanceof PointerInfo p){
          npType = p.getType();
      }
      var convName = getConversionMethodCName(to);
      if(this != npType && !primitive && convName != null /*would return null for nil pointer*/){
          var prefix = "";
          if(!comesFromPointer){
              prefix = "&";
          }
          return convName + "(" + prefix +  expCode + ")";
      }
      return expCode;
  }
  public String ensureCast(TypeInfo to,String expCode){
      return ensureCast(to,expCode,false);
  }
    /**
     *checks if type can be implicitly  converted or is  this one
     */
  @Override
  public boolean equals(Object o){
      if(o == TypeInfo.CHAR && this instanceof NumberInfo n && !n.isFloatingPoint()){
          return true;
      }
      if(this == TypeInfo.CHAR && o instanceof NumberInfo n && !n.isFloatingPoint()){
          return true;
      }
      if(o instanceof InterfaceInfo && ((TypeInfo)o).parents.contains(this)){
          return true;
      }
      return identical((TypeInfo)o);
  }
  protected boolean identical(TypeInfo type){
      return name.equals(type.name);
  }
  public String getConversionMethodCName(TypeInfo type){
      return conversionMethodCNames.get(type);
  }
  public void addConversionMethodCName(TypeInfo type,String cname){
      conversionMethodCNames.put(type,cname);
  }
  public void setPrimaryParent(TypeInfo primaryParent){
      this.primaryParent = primaryParent;
      addParent(primaryParent);
  }
  public TypeInfo getPrimaryParent(){
      return primaryParent;
  }
  public void setDeclaration(String declaration){
      this.declaration = declaration;
  }
  private void addSize(){
      addField("getObjectSize",new UnaryOperator(NumberInfo.MEMORY,"sizeof ",false));
  }
}