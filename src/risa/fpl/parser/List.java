package risa.fpl.parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.tokenizer.TokenType;

public final class List extends AExp{
	private final ArrayList<AExp>exps;
	private final boolean statement;
	public List(int line,int charNum,ArrayList<AExp>exps,boolean statement){
		super(line,charNum);
		this.exps = exps;
		this.statement = statement;
	}
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator superIterator) throws CompilerException,IOException{
	   TypeInfo ret = null;//has to be null see line 27
	   var it = new ExpIterator(exps,getLine(),getTokenNum());
	   var appendSemicolon = false;
	   BuilderWriter b = null;
	   while(it.hasNext()){
		   var exp = it.next();
		   if(exp instanceof Atom atom){
              if(ret == null){
            	  var f =  env.getFunction(atom);
            	  b = new BuilderWriter(writer);
                  ret = f.compile(b,env,it,exp.getLine(),exp.getTokenNum());
                  appendSemicolon = f.appendSemicolon() && statement;
              }else if(atom.getType() == TokenType.ID){
            	 var field = ret.getField(atom.getValue(),env);
            	 if(field == null) {
            		 throw new CompilerException(atom,ret + " has no field called " + atom);
            	 }
            	 field.setPrevCode(b.getCode());
            	 b = new BuilderWriter(writer);
            	 ret = field.compile(b,env,it,atom.getLine(),atom.getTokenNum());
              }
		   }else if(exp instanceof List){
			   exp.compile(writer,env,it);
		   }
	   }
	   if(b != null){
           writer.write(b.getCode());
       }
	   if(appendSemicolon){
	   	 writer.write(";\n");
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