package risa.fpl.parser;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
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
 public abstract TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it)throws IOException,CompilerException;
}