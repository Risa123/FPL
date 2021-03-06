package risa.fpl.function;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;


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
		 if(isCIdChar(c,first)){
		 	b.appendCodePoint(c);
		 }else{
			b.append(Character.getName(c).replace(' ','_')) ;
		 }
		 first = false;
	  }
	  return b.toString();
  }
  TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException;
  default boolean appendSemicolon(){
	  return true;
  }
  static LinkedHashMap<String,TypeInfo> parseTemplateArguments(ExpIterator it,AEnv env)throws CompilerException{
     var args = new LinkedHashMap<String,TypeInfo>();
     for(var arg:getTemplateArguments(it,false)){
         var argType = new TypeInfo(arg.getValue(),"");
         argType.setPrimaryParent(TypeInfo.OBJECT);
         var cls = new ClassInfo(arg.getValue());
         argType.setClassInfo(cls);
         cls.setPrimaryParent(ClassInfo.OBJECT);
         args.put(arg.getValue(),argType);
         env.addType(arg.getValue(),argType);
         env.addFunction(arg.getValue(),new ClassVariable(argType,cls,new TypeInfo[0],""));
     }
     return args;
  }
  static ArrayList<Atom>getTemplateArguments(ExpIterator it,boolean classVariable)throws CompilerException{
      var list = new ArrayList<Atom>();
      while(it.hasNext()){
          var exp = it.peek();
          if(exp instanceof Atom typeID){
              if(typeID.getType() == TokenType.ID){
                  if(list.contains(typeID)){
                      throw new CompilerException(typeID,"duplicate template argument");
                  }
                  list.add(typeID);
                  it.next();
              }else if(typeID.getType() == TokenType.END_ARGS){
                  it.next();
                  break;
              }else if(typeID.getType() == TokenType.CLASS_SELECTOR){
                  if(!classVariable){
                      throw new CompilerException(typeID,"; expected");
                  }
                  break;
              }else{
                  throw new CompilerException(exp,"template argument or ; expected instead of " + typeID);
              }
          }else{
              break;
          }
      }
      return list;
  }
  static ArrayList<TypeInfo>parseTemplateGeneration(ExpIterator it,AEnv env)throws CompilerException{
      return parseTemplateGeneration(it,env,false);
  }
  static ArrayList<TypeInfo>parseTemplateGeneration(ExpIterator it,AEnv env,boolean classVariable)throws CompilerException{
      var args = getTemplateArguments(it,classVariable);
      var types = new ArrayList<TypeInfo>();
      for(var arg:args){
          types.add(env.getType(arg));
      }
      return types;
  }
  static TypeInfo generateTypeFor(TypeInfo template,Atom typeAtom,ExpIterator it,AEnv env,boolean classVariable)throws CompilerException,IOException{
      if(template instanceof TemplateTypeInfo tType){
          return tType.generateTypeFor(parseTemplateGeneration(it,env,classVariable),env,it.getLastLine(),it.getLastCharNum());
      }
      throw new CompilerException(typeAtom,"template type expected instead of " + template);
  }
  String INTERNAL_METHOD_PREFIX = "I";
}