package risa.fpl.parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;

public final class List extends AExp {
	public final ArrayList<AExp>exps;
	private final boolean statement;
	public List(int line, int charNum,ArrayList<AExp>exps,boolean statement) {
		super(line, charNum);
		this.exps = exps;
		this.statement = statement;
	}
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator superIterator) throws CompilerException, IOException {
	   TypeInfo ret = null;
	   var it = new ExpIterator(exps,line,charNum);
	   while(it.hasNext()) {
		   var exp = it.next();
		   if(exp instanceof Atom atom) { 
              if(ret == null) {
            	  var f =  env.getFunction(atom);
                  ret = f.compile(writer, env, it,exp.line,exp.charNum);
                  if(statement && f.appendSemicolon()) {
                	  writer.write(";\n");
                  }
              }else{
            	 var field = ret.getField(atom.value);
            	 if(field == null) {
            		 throw new CompilerException(atom,ret + " has no field called " +atom );
            	 }
            	 ret = field.compile(writer,env,it,atom.line,atom.charNum);
              }
		   }else if(exp instanceof List) {
			   exp.compile(writer, env,it);
		   }
	   }
	   return ret;
	}
	@Override
    public String toString() {
		return exps.toString();
	}
}