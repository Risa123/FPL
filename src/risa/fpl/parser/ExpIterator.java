package risa.fpl.parser;

import java.util.ArrayList;
import java.util.Iterator;

import risa.fpl.CompilerException;
import risa.fpl.tokenizer.TokenType;

public final class ExpIterator {
  private final Iterator<AExp>it;
  private AExp peeked;
  private  int lastLine,lastCharNum;
  public ExpIterator(ArrayList<AExp>exps,int firstLine,int firstCharNum) {
	  this.it = exps.iterator();
	  lastLine = firstLine;
	  lastCharNum = firstCharNum;
  }
  public AExp next() throws CompilerException {
	  if(peeked != null) {
		  var r = peeked;
		  peeked = null;
		  return r;
	  }
	  if(!it.hasNext()){
		  throw new CompilerException(lastLine,lastCharNum,"expression expected");
	  }
	  var exp = it.next();
	  lastLine = exp.getLine();
	  lastCharNum = exp.getCharNum();
	  return exp;
  }
  public Atom nextAtom() throws CompilerException {
	  var exp = next();
	  if(!(exp instanceof Atom)) {
		 throw new CompilerException(exp,"atom expected");
	  }
	  return(Atom) exp;
  }
  public Atom nextID() throws CompilerException {
	  var atom = nextAtom();
	  if(atom.getType() != TokenType.ID) {
		  throw new CompilerException(atom,"identifier expected");
	  }
	  return atom;
  }
  public boolean hasNext() {
	  return it.hasNext() || peeked != null;
  }
  public List nextList() throws CompilerException {
	  var list = next();
	  if(!(list instanceof List)) {
		  throw new CompilerException(list,"list expected");
	  }
	  return (List)list;
  }
  public AExp peek() throws CompilerException {
	  if(peeked == null) {
		  peeked = next();
	  }
	  return peeked;
  }
}