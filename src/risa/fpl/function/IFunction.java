package risa.fpl.function;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;


public interface IFunction {
  static boolean isCId(String id) {
	  var first = true;
	  var count = id.codePointCount(0,id.length());
	  for(int i = 0; i < count;++i) {
		  if(!isCIdChar(id.codePointAt(i),first)) {
			  return false;
		  }
		  if(first) {
			  first = false;
		  }
	  }
	  return true;
  }
  static boolean isCIdChar(int c,boolean first) {
	  return c == '_' || c <= 127 && Character.isAlphabetic(c) || first && Character.isDigit(c);
  }
  static String toCId(String id) {
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
	  }
	  return b.toString();
  }
  TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum) throws IOException,CompilerException;
  default boolean appendSemicolon() {
	  return true;
  }
  String INTERNAL_METHOD_PREFIX = "I";
}