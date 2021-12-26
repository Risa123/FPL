package risa.fpl.parser;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;

public abstract class AExp{
 private final int line,tokenNum;
 public AExp(int line,int tokenNum){
	 this.line = line;
	 this.tokenNum = tokenNum;
 }
 public final int getLine(){
     return line;
 }
 public final int getTokenNum(){
     return tokenNum;
 }
 public abstract TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it)throws CompilerException;
}