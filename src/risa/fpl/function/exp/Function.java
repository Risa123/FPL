package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public class Function implements IField,ICalledOnPointer{
	private final TypeInfo self,returnType;
	private String prevCode;
	private final StringBuilder declaration = new StringBuilder();
	private final AccessModifier accessModifier;
	private final FunctionType type;
	private boolean calledOnPointer;
	private final String name,attrCode;
	private final HashMap<TypeInfo[],FunctionVariant> variants = new HashMap<>();
    public Function(String name,TypeInfo returnType,FunctionType type,TypeInfo self,AccessModifier accessModifier,String attrCode){
       this.returnType = returnType;
       this.accessModifier = accessModifier;
       this.self = self;
       this.type = type;
       this.name = name;
       this.attrCode = attrCode;
    }
    public Function(String name,TypeInfo returnType,FunctionType type,TypeInfo self,AccessModifier accessModifier){
        this(name,returnType,type,self,accessModifier,null);
    }
    @Override
    public AccessModifier getAccessModifier(){
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
		var first = self == null;
		if(self != null){
		    if(calledOnPointer){
		      calledOnPointer = false;
		      b.write("(" + self.getCname() + "*)");
            }else if(!(self instanceof InterfaceInfo) && prevCode != null /*to prevent &this when calling method on implicit this*/){
               if(!self.isPrimitive()){
                   b.write('&');
               }
            }
		    writePrev(b);
		    if(self instanceof InterfaceInfo){
		        b.write(".instance");
            }
        }
		var argList = new ArrayList<TypeInfo>();
		var code = new ArrayList<String>();
		while(it.hasNext()){
		   if(it.peek() instanceof List){
		       break;
           }
		   var exp = it.nextAtom();
		   if(exp.getType() == TokenType.END_ARGS){
			   break;
		   }else if(exp.getType() != TokenType.ARG_SEPARATOR){
			   var buffer = new BuilderWriter(b);
			   var type = exp.compile(buffer,env,it);
               argList.add(type);
               code.add(buffer.getCode());
		   }
		}
		var array = new TypeInfo[argList.size()];
		argList.toArray(array);
		if(!variants.containsKey(array)){
		    throw new CompilerException(line,charNum,"function has no variant with arguments " + Arrays.toString(array));
        }
		var variant = variants.get(array);
        b.write(variant.implName());
        b.write('(');
        for(int i = 0;i < array.length;++i){
            if(first){
                first = false;
            }else{
                b.write(',');
            }
            b.write(array[i].ensureCast(variant.args()[i],code.get(i)));
        }
		b.write(')');
        if(it.hasNext() && returnType != TypeInfo.VOID && it.peek() instanceof Atom a && a.getType() == TokenType.ID){
            var id = it.nextID();
            var field = returnType.getField(id.getValue(),env);
            if(field == null){
                throw new CompilerException(id,returnType + " has no field called " + id);
            }
            field.setPrevCode(b.getCode());
            return field.compile(writer,env,it,id.getLine(),id.getCharNum());
        }
        writer.write(b.getCode());
		return returnType;
	}
    @Override
    public void setPrevCode(String code){
        prevCode = code;
    }
    @Override
    public void writePrev(BufferedWriter writer)throws IOException{
        if(prevCode == null){
           if(self != null){
               writer.write("this");
           }
        }else{
            writer.write(prevCode);
            prevCode = null;
        }
    }
    @Override
    public String getPrevCode(){
        return prevCode;
    }
    public static Function newStatic(String name,TypeInfo returnType,ClassEnv env){
        var cname = "static" + env.getNameSpace()  + IFunction.toCId(name);
        return new Function("new",returnType,FunctionType.NORMAL,null,AccessModifier.PUBLIC);
    }
    public final TypeInfo getReturnType(){
        return returnType;
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
    public boolean hasSignature(Function f){
        if(returnType.equals(f.returnType)){
            for(var args:variants.keySet()){
                if(f.variants.containsKey(args)){
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public boolean equals(Object o){
        if(o instanceof Function f){
            return hasSignature(f);
        }
        return false;
    }
    public Function makeMethod(TypeInfo ofType,String newName,TypeInfo[]variantArgs){
        var args = new TypeInfo[variantArgs.length - 1];
        if(args.length > 0){
            System.arraycopy(variantArgs,1,args,0,args.length);
        }
        var f = new Function(newName,returnType,type,ofType,accessModifier);
        var variant = variants.get(variantArgs);
        f.addVariant(args,variant.cname(),variant.implName());
        return f;
    }
    public Function changeAccessModifier(AccessModifier accessModifier){
        return new Function(getName(),returnType,type,self,accessModifier);
    }
    public void addVariant(TypeInfo[]args,String cname,String implName){
        variants.put(args,new FunctionVariant(args,cname,implName));
        if(type == FunctionType.NATIVE){
            declaration.append("extern ");
        }
        if(accessModifier == AccessModifier.PRIVATE && type != FunctionType.NATIVE){
            declaration.append("static ");
        }
        declaration.append(returnType.getCname());
        declaration.append(' ');
        if(attrCode != null){
            declaration.append(attrCode);
            declaration.append(' ');
        }
        declaration.append(cname);
        declaration.append('(');
        var first = self == null;
        if(self != null){
            declaration.append(self.getCname());
            declaration.append("* this");
        }
        for(var arg:args){
            if(first){
                first = false;
            }else{
                declaration.append(',');
            }
            declaration.append(arg.getCname());
        }
        declaration.append(");\n");
    }
    public String getName(){
        return name;
    }
    public String getDeclaration(){
        return declaration.toString();
    }
    public FunctionVariant getVariant(TypeInfo[]args){
        return variants.get(args);
    }
    public void addVariant(TypeInfo[] args,String cname,StringBuilder macroCode){
        variants.put(args,new FunctionVariant(args,cname,cname));
        declaration.append(macroCode);
    }
    public ArrayList<TypeInfo>getRequiredTypes(){
        var list = new ArrayList<TypeInfo>();
        list.add(returnType);
        for(var args:variants.keySet()){
            list.addAll(Arrays.stream(args).toList());
        }
        return list;
    }
}