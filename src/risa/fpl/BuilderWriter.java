package risa.fpl;

import java.io.BufferedWriter;
import java.io.Writer;

public final class BuilderWriter extends BufferedWriter{
 private final StringBuilder builder = new StringBuilder();
 private static final Writer NULL_WRITER = Writer.nullWriter();
 public BuilderWriter(){
     super(NULL_WRITER);
 }
 @Override
 public void write(int c){
	builder.append((char)c);
 }
 @Override
 public void write(String str){
	builder.append(str);
 }
 public String getCode(){
	 return builder.toString();
 }
}