package risa.fpl;

import java.io.BufferedWriter;
import java.io.IOException;

public final class BuilderWriter extends BufferedWriter {
  private final StringBuilder builder = new StringBuilder();	
  public BuilderWriter(BufferedWriter writer) {
	  super(writer);
  }

 @Override
 public void write(int c) throws IOException {
	builder.append((char)c);
 }

 @Override
 public void write(String str) throws IOException {
	builder.append(str);
 }
 public String getText() {
	 return builder.toString();
 }
}