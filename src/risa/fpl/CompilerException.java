package risa.fpl;

import risa.fpl.parser.AExp;

public final class CompilerException extends Exception{
  private String sourceFile;	
  public CompilerException(int line,int charNum,String msg) {
	  super(line + ":" + charNum + ":" + msg);
  }
  public CompilerException(AExp exp,String msg) {
	  this(exp.getLine(),exp.getCharNum(),msg);
  }
  public void setSourceFile(String sourceFile) {
      if(this.sourceFile == null){
          this.sourceFile = sourceFile;
      }
  }
  @Override
  public String getMessage() {
	  return sourceFile + ":" + super.getMessage();
  }
}