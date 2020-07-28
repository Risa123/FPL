package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public class Function extends TypeInfo implements IFunction {
	public final String declaration;
	public final TypeInfo returnType;
	public final TypeInfo[]args;
    public Function(String name,TypeInfo returnType,String cname,TypeInfo[] args,boolean extern) {
       super(cname,name);
       this.returnType = returnType;
       this.args = args;
       var b = new StringBuilder();
       if(extern) {
    	   b.append("extern ");
       }
       b.append(returnType.cname);
       b.append(' ');
       b.append(cname);
       b.append('(');
       var first = true;
       for(var arg:args) {
    	   if(first) {
    		   first = false;
    	   }else {
    		   b.append(',');
    	   }
    	   b.append(arg.cname);
       }
       b.append(");\n");
       declaration = b.toString();
    }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		writer.write(cname);
		writer.write('(');
		var args = new ArrayList<TypeInfo>(this.args.length);
		var first = true;
		while(it.hasNext()) {
		   var exp = it.nextAtom();
		   if(exp.type == TokenType.ARG_SEPARATOR) {
			   
		   }else if(exp.type == TokenType.END_ARGS) {
			   break;
		   }else {
			   if(first) {
				   first = false;
			   }else {
				   writer.write(',');
			   }
			   args.add(exp.compile(writer, env, it));
		   }
		}
		if(!Arrays.equals(this.args,args.toArray())) {
			throw new CompilerException(line,charNum,Arrays.toString(this.args) + " expected as arguments instead of " + args);
		}
		writer.write(')');
		return returnType;
	}
}