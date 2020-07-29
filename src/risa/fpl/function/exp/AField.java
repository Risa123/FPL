package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.function.IFunction;

public abstract class AField implements IFunction{
  String prev_code;
  protected void writePrev(BufferedWriter writer) throws IOException {
	  if(prev_code != null) {
		  writer.write(prev_code);
		  prev_code = null;
	  }
  }
}