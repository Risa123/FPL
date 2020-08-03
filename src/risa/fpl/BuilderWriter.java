package risa.fpl;

import java.io.BufferedWriter;

public final class BuilderWriter extends BufferedWriter {
  private final StringBuilder builder = new StringBuilder();	
  public BuilderWriter(BufferedWriter writer) {
	  super(writer);
  }

 @Override
 public void write(int c){
	builder.append((char)c);
 }

 @Override
 public void write(String str){
	builder.append(str);
 }
 public String getText() {
	 return builder.toString();
 }
}