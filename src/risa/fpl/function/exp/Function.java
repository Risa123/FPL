package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public class Function extends TypeInfo implements IField {
	private final TypeInfo returnType;
	private final TypeInfo[]args;
	private final TypeInfo self;
	private String prev_code;
	private final AccessModifier accessModifier;
	private final FunctionType type;
	private boolean calledOnPointer;
    public Function(String name, TypeInfo returnType, String cname, TypeInfo[] args,FunctionType type, TypeInfo self, AccessModifier accessModifier, AEnv env) {
       super(name,cname,true);
       this.returnType = returnType;
       this.args = args;
       this.accessModifier = accessModifier;
       this.self = self;
       this.type = type;
        if(type == FunctionType.NATIVE) {
            appendToDeclaration("extern ");
        }
        appendToDeclaration(returnType.getCname());
        appendToDeclaration(' ');
        appendToDeclaration(cname);
        appendToDeclaration('(');
        var first = self == null;
        if(self != null){
            appendToDeclaration(self.getCname());
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
        buildDeclaration(env);
    }

    @Override
    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    @Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        if(self != null && isVirtual()){
            writer.write(getPrevCode());
            if(self instanceof InterfaceInfo){
                writer.write(".impl->");
            }else{
                writer.write("->class_data->");
            }
        }
		writer.write(getCname());
		writer.write('(');
		var first = self == null;
		if(self != null){
		    if(calledOnPointer){
		      calledOnPointer = false;
            }else if(!(self instanceof InterfaceInfo)){
                writer.write('&');
            }
		    writePrev(writer);
		    if(self instanceof InterfaceInfo){
		        writer.write(".instance");
            }
        }
		int argCount = 0;
		while(it.hasNext()) {
		   var test = it.peek();
		   if(test instanceof List){
		       break;
           }
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
			   var buffer = new BuilderWriter(writer);
			   var type = exp.compile(buffer, env, it);
               if(!args[argCount].equals(type)){
                   throw new CompilerException(exp,"incorrect argument " + Arrays.toString(args) + " are expected arguments");
               }
			   writer.write(type.ensureCast(args[argCount],buffer.getCode()));
			   argCount++;
		   }
		}
		if(argCount != args.length) {
			throw new CompilerException(line,charNum,"incorrect number of arguments (" + argCount + ")expected" + Arrays.toString(args));
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
           if(self != null){
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
    public static Function newNew(String cname,TypeInfo type,TypeInfo[]args,AEnv env){
        return new Function("new",new PointerInfo(type),cname,args,FunctionType.NORMAL,null,AccessModifier.PUBLIC,env);
    }
    public final TypeInfo getReturnType(){
        return returnType;
    }
    public final TypeInfo[]getArguments(){
        return args;
    }
    public FunctionType getType(){
        return type;
    }
    public boolean isVirtual(){
        return type == FunctionType.ABSTRACT || type == FunctionType.VIRTUAL;
    }
    public void calledOnPointer(){
        calledOnPointer = true;
    }
    public TypeInfo getSelf(){
        return self;
    }
    public boolean equalSignature(Function f){
        return returnType.equals(f.returnType) && Arrays.equals(args,f.args);
    }
}