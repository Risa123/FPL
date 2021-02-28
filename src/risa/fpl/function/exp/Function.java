package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public class Function extends TypeInfo implements IField,ICalledOnPointer{
	private final TypeInfo[]args;
	private final TypeInfo self,returnType;
	private String prev_code;
	private final AccessModifier accessModifier;
	private final FunctionType type;
	private boolean calledOnPointer;
	private final String implName;
    public Function(String name,TypeInfo returnType,String cname,TypeInfo[] args,FunctionType type,TypeInfo self,AccessModifier accessModifier,String implName,String attrCode){
       super(name,cname,true);
       this.returnType = returnType;
       this.args = args;
       this.accessModifier = accessModifier;
       this.self = self;
       this.type = type;
       this.implName = implName;
        if(type == FunctionType.NATIVE) {
            appendToDeclaration("extern ");
        }
        appendToDeclaration(returnType.getCname());
        appendToDeclaration(' ');
        appendToDeclaration(attrCode);
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
        buildDeclaration();
    }
    public Function(String name,TypeInfo returnType,String cname,TypeInfo[] args,FunctionType type,TypeInfo self,AccessModifier accessModifier,String implName){
        this(name,returnType,cname,args,type,self,accessModifier,implName,"");
    }
    @Override
    public AccessModifier getAccessModifier() {
        return accessModifier;
    }
    @Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var b = new BuilderWriter(writer);
        if(isVirtual()){
            if(self instanceof InstanceInfo i){
                b.write("((" + i.getClassDataType() + ")");
            }
            if(getPrevCode() == null){
                b.write("this");
            }else{
                b.write(getPrevCode());
            }
            if(self instanceof InterfaceInfo){
                b.write(".impl->");
            }else{
                if(calledOnPointer || getPrevCode() == null){
                    b.write("->");
                }else{
                    b.write('.');
                }
                b.write("object_data)->");
            }
        }
		b.write(implName);
		b.write('(');
		var first = self == null;
		if(self != null){
		    if(calledOnPointer){
		      calledOnPointer = false;
		      b.write("(" + self.getCname() + "*)");
            }else if(!(self instanceof InterfaceInfo) && prev_code != null /*to prevent &this when calling method on implicit this*/){
                b.write('&');
            }
		    writePrev(b);
		    if(self instanceof InterfaceInfo){
		        b.write(".instance");
            }
        }
		var argList = new ArrayList<TypeInfo>();
		while(it.hasNext()){
		   if(it.peek() instanceof List){
		       break;
           }
		   var exp = it.nextAtom();
		   if(exp.getType() == TokenType.END_ARGS) {
			   break;
		   }else if(exp.getType() != TokenType.ARG_SEPARATOR){
			   if(first) {
				   first = false;
			   }else {
				   b.write(',');
			   }
			   var buffer = new BuilderWriter(b);
			   var type = exp.compile(buffer,env,it);
			   b.write(type.ensureCast(type,buffer.getCode()));
			   argList.add(type);
		   }
		}
		var array = new TypeInfo[argList.size()];
		argList.toArray(array);
		if(!Arrays.equals(args,array)){
		    throw new CompilerException(line,charNum,"incorrect arguments expected" + Arrays.toString(args) + " instead of " + Arrays.toString(array));
        }
		b.write(')');
        if(it.hasNext() && returnType != TypeInfo.VOID && it.peek() instanceof Atom a && a.getType() == TokenType.ID){
            var id = it.nextID();
            var field = returnType.getField(id.getValue(),env);
            if(field == null){
                throw new CompilerException(id,returnType + " has no field called " + id);
            }
            if(field instanceof Cast){
                field.setPrevCode(b.getCode());
            }else{
                writer.write(b.getCode());
            }
            return field.compile(writer,env,it,id.getLine(),id.getCharNum());
        }
        writer.write(b.getCode());
		return returnType;
	}
    @Override
    public void setPrevCode(String code){
        prev_code = code;
    }
    @Override
    public void writePrev(BufferedWriter writer)throws IOException{
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
    public String getPrevCode(){
        return prev_code;
    }
    public static Function newStatic(String name,TypeInfo returnType,TypeInfo[]args,ClassEnv env){
        var cname = "static" + env.getNameSpace()  + IFunction.toCId(name);
        return new Function("new",returnType,cname,args,FunctionType.NORMAL,null,AccessModifier.PUBLIC,cname);
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
    public String getImplName(){
        return implName;
    }
    @Override
    public boolean equals(Object o){
        if(o instanceof Function f){
            return equalSignature(f);
        }
        return false;
    }
    public Function makeMethod(TypeInfo ofType){
        var args = new TypeInfo[this.args.length - 1];
        if (args.length > 0) {
            System.arraycopy(this.args,1,args,0,args.length);
        }
        return new Function(getName(),returnType,getCname(),args,type,new PointerInfo(ofType),accessModifier,implName);
    }
    public Function changeAccessModifier(AccessModifier accessModifier){
        return new Function(getName(),returnType,getCname(),args,type,self,accessModifier,implName);
    }
}