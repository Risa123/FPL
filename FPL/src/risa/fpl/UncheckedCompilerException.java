package risa.fpl;

public final class UncheckedCompilerException extends RuntimeException {
  public final CompilerException ex;
  public UncheckedCompilerException(CompilerException ex) {
	  this.ex = ex;
  }
}