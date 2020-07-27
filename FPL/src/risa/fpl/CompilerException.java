package risa.fpl;

import risa.fpl.parser.AExp;

public final class CompilerException extends Exception {
  private String sourceFile;	
  public CompilerException(int line,int charNum,String msg) {
	  super(line + ":" + charNum + ":" + msg);
  }
  public CompilerException(AExp exp,String msg) {
	  this(exp.line,exp.charNum,msg);
  }
  public void setSourceFile(String sourceFile) {
	  this.sourceFile = sourceFile;
  }
  @Override
  public String getMessage() {
	  return sourceFile + ":" + super.getMessage();
  }
}