package risa.fpl.function;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;


public interface IFunction{
  static boolean notCID(String id){
	  var first = true;
	  var count = id.codePointCount(0,id.length());
	  for(int i = 0; i < count;++i) {
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
	  for(int i = 0; i < count;++i) {
		  var c = id.codePointAt(i);
		 if(isCIdChar(c,first)) {
		 	b.appendCodePoint(c);
		 }else {
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
     for(var arg:getTemplateArguments(it)){
         args.put(arg.getValue(),TypeInfo.OBJECT);
         env.addType(arg.getValue(),TypeInfo.OBJECT);
     }
     return args;
  }
  static ArrayList<Atom>getTemplateArguments(ExpIterator it)throws CompilerException{
      var list = new ArrayList<Atom>();
      while(it.hasNext()){
          var exp = it.next();
          if(exp instanceof Atom typeID){
              if(typeID.getType() == TokenType.ID){
                  if(list.contains(typeID)){
                      throw new CompilerException(typeID,"duplicate template argument");
                  }
                  list.add(typeID);
              }else if(typeID.getType() == TokenType.END_ARGS){
                  break;
              }else{
                  throw new CompilerException(exp,"template argument or ; expected");
              }
          }else{
              throw new CompilerException(exp,"template argument or ; expected");
          }
      }
      return list;
  }
  String INTERNAL_METHOD_PREFIX = "I";
}