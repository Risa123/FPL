package risa.fpl.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.SubEnv;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;


public interface IFunction{
  static boolean notCID(String id){
	  var first = true;
	  var count = id.codePointCount(0,id.length());
	  for(int i = 0; i < count;++i){
		  if(!isCIdChar(id.codePointAt(i),first)){
			  return true;
		  }
		  if(first){
			  first = false;
		  }
	  }
	  return false;
  }
  static boolean isCIdChar(int c,boolean first){
	  return c == '_' || c <= 127 && Character.isAlphabetic(c) || !first && Character.isDigit(c);
  }
  static String toCId(String id){
	  var b = new StringBuilder("_");
	  var count = id.codePointCount(0,id.length());
	  var first = true;
	  for(int i = 0; i < count;++i){
		  var c = id.codePointAt(i);
          b.append(isCIdChar(c,first)?(char)c:Character.getName(c).replace(' ','_'));
		  first = false;
	  }
	  return b.toString();
  }
  TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException;
  default boolean appendSemicolon(){
	  return true;
  }
  default LinkedHashMap<String,TypeInfo>parseTemplateArguments(ExpIterator it,AEnv env)throws CompilerException{
     var args = new LinkedHashMap<String,TypeInfo>();
     var lastLine = it.getLastLine();
     var lastChar = it.getLastCharNum();
     var list = new ArrayList<Atom>();
     while(it.hasNext()){
          var exp = it.peek();
          if(exp instanceof Atom typeID){
              if(typeID.getType() == AtomType.ID){
                  if(list.contains(typeID)){
                      error(typeID,"duplicate template argument");
                  }
                  list.add(typeID);
                  it.next();
              }else if(typeID.getType() == AtomType.END_ARGS){
                  it.next();
                  break;
              }else{
                  error(exp,"template argument or ; expected instead of " + typeID);
              }
          }else{
              break;
          }
      }
     for(var arg:list){
         var argType = new TypeInfo(arg.getValue(),""){
             @Override
             public boolean equals(Object o){
                 return o instanceof TypeInfo t && getPrimaryParent() == t.getPrimaryParent();
             }
         };
         argType.setPrimaryParent(TypeInfo.OBJECT);
         var cls = new ClassInfo(arg.getValue());
         argType.setClassInfo(cls);
         cls.setPrimaryParent(ClassInfo.OBJECT);
         args.put(arg.getValue(),argType);
         env.addType(argType);
     }
     if(args.isEmpty()){
         error(lastLine,lastChar,"more than one argument expected");
     }
     return args;
  }

  default ArrayList<Object>parseTemplateGeneration(ExpIterator it,AEnv env)throws CompilerException{
      return parseTemplateGeneration(it,env,false);
  }
  @SuppressWarnings("ConstantConditions")
  default ArrayList<Object>parseTemplateGeneration(ExpIterator it, AEnv env, boolean classVariable)throws CompilerException{
      var args = new ArrayList<>();
      while(it.hasNext()){
          var exp = it.peek();
          if(exp instanceof Atom typeID){
              if(typeID.getType() == AtomType.ID){
                  it.next();
                  Object arg = env.getType(typeID);
                  if(arg instanceof TemplateTypeInfo t){
                     if(it.peek() instanceof Atom a && a.getType() == AtomType.END_ARGS){
                         it.next();
                         arg = new TemplateArgument(t,parseTemplateGeneration(it,env,classVariable));
                     }else{
                         error(typeID,"template arguments expected");
                     }
                  }
                  args.add(arg);
              }else if(typeID.getType() == AtomType.END_ARGS){
                  it.next();
                  break;
              }else if(typeID.getType() == AtomType.CLASS_SELECTOR){
                  if(!classVariable){
                      error(typeID,"; expected");
                  }
                  break;
              }else{
                  error(exp,"template argument or ; expected instead of " + typeID);
              }
          }else{
              break;
          }
      }
      return args;
  }
  default InstanceInfo generateTypeFor(TypeInfo template,Atom typeAtom,ExpIterator it,AEnv env,boolean classVariable)throws CompilerException{
      if(template instanceof TemplateTypeInfo tType){
          try{
              return tType.generateTypeFor(parseTemplateGeneration(it,env,classVariable),env,it.getLastLine(),it.getLastCharNum());
          }catch(IOException e){
              throw new UncheckedIOException(e);
          }
      }
      throw new CompilerException(typeAtom,"template type expected instead of " + template);
  }
  static String createTemplateTypeCname(String cname,TypeInfo[] argsInfo){
      var cName = new StringBuilder(cname);
      for(var arg:argsInfo){
          var chars = arg.getCname().toCharArray();
          var b = new StringBuilder();
          for(char c:chars){
              if(Character.isAlphabetic(c) || Character.isDigit(c)|| c == '_' || c == '-'){
                  b.append(c);
              }else if(c == ' '){
                  b.append('_');
              }else{
                  b.append(Character.getName(c).replace(' ', '_'));
              }
          }
          cName.append(b);
      }
      return cName.toString();
  }
  default void error(AExp atom, String message)throws CompilerException{
      throw new CompilerException(atom,message);
  }
  default void error(int line,int tokenNum,String message)throws CompilerException{
      throw new CompilerException(line,tokenNum,message);
  }
  /**
   Prefix for methods, variables and types  which are used by generated C code internally.
   **/
  String INTERNAL_PREFIX = "I";
}