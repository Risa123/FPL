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
  public static final TypeInfo VOID = new TypeInfo("void","void");
  public static final TypeInfo OBJECT = new TypeInfo("object",null){
      @Override
      public void setClassInfo(ClassInfo info){
          addField("getObjectSize",new ValueExp(NumberInfo.MEMORY,null));
          super.setClassInfo(info);
      }
  };
  public static final TypeInfo BOOL = new TypeInfo("bool","char");
  public static final TypeInfo CHAR = new TypeInfo("char","char");
  public static final TypeInfo NIL = new TypeInfo("nil",null);
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
      BOOL.addField("?",new TertiaryOperator());
	  NIL.addField("==",new BinaryOperator(BOOL,NIL,"=="));
      NIL.addField("!=",new BinaryOperator(BOOL,NIL,"!="));
      BOOL.addSize();
      CHAR.addSize();
      OBJECT.addField("==",new BinaryOperator(BOOL,OBJECT,"&=="));
      OBJECT.addField("!=",new BinaryOperator(BOOL,OBJECT,"&!="));
  }
  private final String name,cname;
  protected final HashMap<String,AField>fields = new HashMap<>();
  private ClassInfo classInfo;
  protected final ArrayList<TypeInfo>parents = new ArrayList<>(),requiredTypes = new ArrayList<>();
  private TypeInfo primaryParent;
  public TypeInfo(String name,String cname){
	  this.name = name;
	  this.cname = cname;
  }
  @Override
  public String toString(){
	  return name;
  }
  public void addField(String name,AField value){
	  fields.put(name,value);
  }
  //returns null if field cannot be accessed from Env from
  public AField getField(String name,AEnv from){
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
              if(field.getAccessModifier() == AccessModifier.PRIVATE && e.getClassInfo() != null && e.getClassInfo() == classInfo){
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
      info.setInstanceInfo(this);
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
  public String getDeclaration(){
      return "";
  }
  protected final void addRequiredType(TypeInfo type){
      if(type != this && !type.isPrimitive() && type.notIn(requiredTypes)){
         requiredTypes.add(type);
      }else if(type instanceof PointerInfo p && p.getType() != this && !p.getType().isPrimitive() && p.getType().notIn(requiredTypes)){
         requiredTypes.add(p.getType());
      }
  }
  public final boolean notIn(Collection<TypeInfo>types){
        for(var t:types){
            if(t == this){
                return false;
            }
        }
        return true;
    }
  public boolean isPrimitive(){
      return true;
  }
  public void addParent(TypeInfo parent){
      parents.add(parent);
  }
  public final ArrayList<TypeInfo>getRequiredTypes(){
      return requiredTypes;
  }
  public final ArrayList<TypeInfo>getParents(){
      return parents;
  }
  public final HashMap<FunctionVariant,Function>getMethodVariantsOfType(FunctionType type){
      var result = new HashMap<FunctionVariant,Function>();
      for(var field:fields.values()){
          if(field instanceof Function f){
              for(var v:f.getVariants()){
                  if(v.getType() == type){
                      result.put(v,f);
                  }
              }
          }
      }
      for(var parent:parents){
          for(var entry:parent.getMethodVariantsOfType(type).entrySet()){
             var found = false;
             for(var e:result.entrySet()){
                 if(e.getValue().getName().equals(entry.getValue().getName())){
                     found = true;
                     break;
                 }
             }
             if(!found){
                 result.put(entry.getKey(),entry.getValue());
             }
          }
      }
      return result;
  }
  public boolean notIntegerNumber(){
      return true;
  }
  public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer,boolean notReturnedByFunction,SubEnv env){
      if(expCode.endsWith(";\n")){ //caused by Var
          expCode = expCode.substring(0,expCode.length() - 2);
      }
      var npType = to instanceof PointerInfo p?p.getType():to;
      if(this != npType && this instanceof InstanceInfo instance && npType instanceof InterfaceInfo i && parents.contains(i)){
          var prefix = "";
          var postfix = "";
          if(!comesFromPointer){
            if(notReturnedByFunction){
                prefix = "&";
            }else{
                prefix = instance.getToPointerName() + '(';
                postfix = ",&" + env.getToPointerVarName(instance) + ')';
            }
          }
          return instance.getConversionMethod(i) + '(' + prefix +  expCode + postfix + ')';
      }
      return expCode;
  }
  public String ensureCast(TypeInfo to,String expCode,SubEnv env){
      return ensureCast(to,expCode,false,true,env);
  }
  public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer,SubEnv env){
      return ensureCast(to,expCode,comesFromPointer,true,env);
  }
  /**
   *checks if type can be implicitly  converted or is  this one
   **/
  @Override
  public boolean equals(Object o){
      if((this == TypeInfo.CHAR && o instanceof NumberInfo n && n.getSize() == 1)||(o == TypeInfo.CHAR && this instanceof NumberInfo n1 && n1.getSize() == 1)){
          return true;
      }
      return o instanceof InterfaceInfo && ((TypeInfo)o).parents.contains(this) || this == OBJECT || this == o;
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
  public final AField getFieldFromThisType(String name){
      return fields.get(name);
  }
  public final HashMap<String,AField>getFields(){
      return fields;
  }
}