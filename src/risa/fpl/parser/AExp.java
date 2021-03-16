package risa.fpl.parser;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;

public abstract class AExp{
 private final int line,charNum;
 public AExp(int line,int charNum){
	 this.line = line;
	 this.charNum = charNum;
 }
 public final int getLine(){
     return line;
 }
 public final int getCharNum(){
     return charNum;
 }
 public abstract TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it) throws IOException,CompilerException;
}