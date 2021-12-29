package risa.fpl.parser;

import java.util.ArrayList;
import java.util.Iterator;

import risa.fpl.CompilerException;

public final class ExpIterator{
  private final Iterator<AExp>it;
  private AExp peeked;
  private  int lastLine,lastCharNum;
  public ExpIterator(ArrayList<AExp>exps,int firstLine,int firstCharNum){
	  this.it = exps.iterator();
	  lastLine = firstLine;
	  lastCharNum = firstCharNum;
  }
  public AExp next()throws CompilerException{
	  if(peeked != null){
		  var r = peeked;
		  peeked = null;
		  return r;
	  }
	  if(!it.hasNext()){
		  throw new CompilerException(lastLine,lastCharNum,"expression expected");
	  }
	  var exp = it.next();
	  lastLine = exp.getLine();
	  lastCharNum = exp.getTokenNum();
	  return exp;
  }
  public Atom nextAtom()throws CompilerException{
	  var exp = next();
	  if(exp instanceof Atom a){
		 return a;
	  }
	  throw new CompilerException(exp,"atom expected");
  }
  public Atom nextID()throws CompilerException{
	  var atom = nextAtom();
	  if(atom.getType() != AtomType.ID){
		  throw new CompilerException(atom,"identifier expected instead of " + atom);
	  }
	  return atom;
  }
  public boolean hasNext(){
	  return it.hasNext() || peeked != null;
  }
  public List nextList()throws CompilerException{
	  var list = next();
	  if(list instanceof List l){
		  return l;
	  }
	  throw new CompilerException(list,"list expected");
  }
  public AExp peek()throws CompilerException{
	  if(peeked == null){
		  peeked = next();
	  }
	  return peeked;
  }
  public boolean checkTemplate()throws CompilerException{
        var result = hasNext() && peek() instanceof Atom atom && atom.getType() == AtomType.END_ARGS;
        if(result){
            next();
        }
        return result;
    }
    public int getLastLine(){
      return lastLine;
    }
    public int getLastCharNum(){
      return lastCharNum;
    }
}