package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public class Function extends TypeInfo implements IFunction,IField {
	public final TypeInfo returnType;
	public final TypeInfo[]args;
	private final boolean method;
	private String prev_code;
    public Function(String name,TypeInfo returnType,String cname,TypeInfo[] args,boolean extern,TypeInfo methodOwner) {
       super(name,cname,buildDeclaration(cname, returnType,args,extern,methodOwner),null);
       this.returnType = returnType;
       this.args = args;
       this.method = methodOwner != null;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		writer.write(cname);
		writer.write('(');
		var args = new ArrayList<TypeInfo>(this.args.length);
		var first = !method;
		if(method){
		    writer.write('&');
		    writePrev(writer);
        }
		while(it.hasNext()) {
		   var exp = it.nextAtom();
		   if(exp.getType() == TokenType.ARG_SEPARATOR) {
			   
		   }else if(exp.getType() == TokenType.END_ARGS) {
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
	private static String buildDeclaration(String cname,TypeInfo returnType,TypeInfo[]args,boolean extern,TypeInfo methodOwner){
        var b = new StringBuilder();
        if(extern) {
            b.append("extern ");
        }
        b.append(returnType.cname);
        b.append(' ');
        b.append(cname);
        b.append('(');
        var first = methodOwner == null;
        if(methodOwner != null){
            b.append(methodOwner.cname);
            b.append("* this");
        }
        for(var arg:args) {
            if(first) {
                first = false;
            }else {
                b.append(',');
            }
            b.append(arg.cname);
        }
        b.append(");\n");
        return b.toString();
    }

    @Override
    public void setPrevCode(String code) {
        prev_code = code;
    }
    @Override
    public void writePrev(BufferedWriter writer) throws IOException {
        if(prev_code != null){
            writer.write(prev_code);
            prev_code = null;
        }
    }
    @Override
    public String getPrevCode() {
        return prev_code;
    }
    public static Function newNew(String cname,TypeInfo type){
        return new Function("new",new PointerInfo(type),cname,new TypeInfo[]{},false,null);
    }
}