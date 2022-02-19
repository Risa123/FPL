package risa.fpl.parser;

import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;

public final class List extends AExp{
	private final ArrayList<AExp>exps;
	private final boolean statement;
	public List(int line,int charNum,ArrayList<AExp>exps,boolean statement){
		super(line,charNum);
		this.exps = exps;
		this.statement = statement;
	}
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator superIterator)throws CompilerException{
	   TypeInfo ret = null;//has to be null see line 27
	   var it = new ExpIterator(exps,getLine(),getTokenNum());
	   var appendSemicolon = false;
	   StringBuilder b = null;
	   while(it.hasNext()){
		   var exp = it.next();
		   if(exp instanceof Atom atom){
              if(ret == null){
            	  var f = env.getFunction(atom);
            	  b = new StringBuilder();
                  ret = f.compile(b,env,it,exp.getLine(),exp.getTokenNum());
                  appendSemicolon = f.appendSemicolon() && statement;
              }else if(atom.getType() == AtomType.ID){
            	 var field = ret.getField(atom.getValue(),env);
            	 if(field == null) {
            		 throw new CompilerException(atom,ret + " has no field called " + atom);
            	 }
            	 field.setPrevCode(b.toString());
            	 b = new StringBuilder();
            	 ret = field.compile(b,env,it,atom.getLine(),atom.getTokenNum());
              }
		   }else{
			   exp.compile(builder,env,it);
		   }
	   }
	   if(b != null){
           builder.append(b);
       }
	   if(appendSemicolon){
	   	 builder.append(";\n");
	   }
	   return ret;
	}
	@Override
    public String toString(){
		return exps.toString();
	}
	public ArrayList<AExp>getExps(){
	    return exps;
    }
}