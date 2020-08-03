package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public class Function extends TypeInfo implements IField {
	private final TypeInfo returnType;
	private final TypeInfo[]args;
	private final boolean method;
	private String prev_code;
	private final AccessModifier accessModifier;
    public Function(String name,TypeInfo returnType,String cname,TypeInfo[] args,boolean extern,TypeInfo methodOwner,AccessModifier accessModifier) {
       super(name,cname);
       this.returnType = returnType;
       this.args = args;
       this.accessModifier = accessModifier;
       this.method = methodOwner != null;
        if(extern) {
            appendToDeclaration("extern ");
        }
        appendToDeclaration(returnType.getCname());
        appendToDeclaration(' ');
        appendToDeclaration(cname);
        appendToDeclaration('(');
        var first = methodOwner == null;
        if(methodOwner != null){
            appendToDeclaration(methodOwner.getCname());
            appendToDeclaration("* this");
        }
        for(var arg:args) {
            if(first) {
                first = false;
            }else {
                appendToDeclaration(',');
            }
            appendToDeclaration(arg.getCname());
        }
        appendToDeclaration(");\n");
        buildDeclaration();
    }

    @Override
    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    @Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		writer.write(getCname());
		writer.write('(');
		var args = new ArrayList<TypeInfo>(this.args.length);
		var first = !method;
		if(method){
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
    @Override
    public void setPrevCode(String code) {
        prev_code = code;
    }
    @Override
    public void writePrev(BufferedWriter writer) throws IOException {
        if(prev_code == null){
           if(method){
               writer.write("this");
           }
        }else{
            writer.write(prev_code);
            prev_code = null;
        }
    }
    @Override
    public String getPrevCode() {
        return prev_code;
    }
    public static Function newNew(String cname,TypeInfo type,TypeInfo[]args){
        return new Function("new",new PointerInfo(type),cname,args,false,null,AccessModifier.PUBLIC);
    }
    public final TypeInfo getReturnType(){
        return returnType;
    }
    public final TypeInfo[]getArguments(){
        return args;
    }

}