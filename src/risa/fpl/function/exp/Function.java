package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.info.*;
import risa.fpl.parser.AExp;
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
	private int callStatus;
	private boolean asFunctionPointer;
	public static final int NO_STATUS = 0,CALLED_ON_POINTER = 1,CALLED_ON_RETURNED_INSTANCE = 2,CALLED_ON_INSTANCE_R_BY_FUNC = 3;
	private final String name,attrCode;
	private final ArrayList<FunctionVariant>variants = new ArrayList<>();
	private final ArrayList<TemplateVariant>templateVariants = new ArrayList<>();
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
    public Function(String name,TypeInfo returnType,AccessModifier accessModifier){
        this(name,returnType,FunctionType.NORMAL,null,accessModifier);
    }
    @Override
    public final AccessModifier getAccessModifier(){
        return accessModifier;
    }
    @Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var b = new BuilderWriter(writer);
		var argList = new ArrayList<TypeInfo>();
		var returnedData = new ArrayList<ReturnedData>();
		if(self instanceof InstanceInfo i && i.isException() && name.equals("throw")){
           if(env instanceof FnSubEnv subEnv){
               subEnv.getReturnType();//to prevent no return error
           }
        }
		while(it.hasNext() && !(it.peek() instanceof List)){
		   var exp = it.nextAtom();
		   if(exp.getType() == TokenType.END_ARGS){
			   break;
		   }else if(exp.getType() != TokenType.ARG_SEPARATOR){
			   var buffer = new BuilderWriter(b);
			   var f = env.getFunction(exp);
               argList.add(f.compile(buffer,env,it,exp.getLine(),exp.getTokenNum()));
               returnedData.add(new ReturnedData(buffer.getCode(),!(f instanceof  Function)));
		   }
		}
		var array = new TypeInfo[argList.size()];
		argList.toArray(array);
		if(!hasVariant(array)){
		    throw new CompilerException(line, tokenNum,"function has no variant with arguments " + Arrays.toString(array));
        }
		var variant = getVariant(array);
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
                if(callStatus == CALLED_ON_POINTER || getPrevCode() == null){
                    b.write("->");
                }else{
                    b.write('.');
                }
                b.write("objectData)->");
            }
        }
        if(asFunctionPointer){
            asFunctionPointer = false;
        }else{
            b.write(variant.implName());
        }
        b.write('(');
		var first = self == null;
		var ref = false;
		if(self != null){
		    if(callStatus == CALLED_ON_POINTER){
		      callStatus = NO_STATUS;
            }else if(!(self instanceof InterfaceInfo) && prevCode != null /*to prevent &this when calling method on implicit this*/){
               if(!self.isPrimitive()){
                   if(callStatus == CALLED_ON_RETURNED_INSTANCE || callStatus == CALLED_ON_INSTANCE_R_BY_FUNC){
                     if(callStatus == CALLED_ON_RETURNED_INSTANCE){
                         callStatus = NO_STATUS;
                     }
                   }else{
                       ref = true;
                       b.write("(&");
                   }
               }
            }
            writePrev(b);
		    if(ref){
                b.write(')');
            }
		    if(callStatus == CALLED_ON_INSTANCE_R_BY_FUNC){
		        b.write(')');
		        callStatus = NO_STATUS;
            }
            if(self instanceof InterfaceInfo){
                b.write(".instance");
            }
        }
        for(int i = 0;i < array.length;++i){
            if(first){
                first = false;
            }else{
                b.write(',');
            }
            var comesFromPointer = array[i] instanceof PointerInfo;
            b.write(array[i].ensureCast(variant.args()[i],returnedData.get(i).code,comesFromPointer,returnedData.get(i).notReturnedByFunction));
        }
		b.write(')');
        if(it.hasNext() && returnType != TypeInfo.VOID && it.peek() instanceof Atom a && a.getType() == TokenType.ID){
            var id = it.nextID();
            var field = returnType.getField(id.getValue(),env);
            if(field == null){
                throw new CompilerException(id,returnType + " has no field called " + id);
            }
            field.setPrevCode(b.getCode());
            if(field instanceof Function f && returnType instanceof InstanceInfo i){
               f.callStatus = CALLED_ON_INSTANCE_R_BY_FUNC;
               var c = i.getToPointerName() + "(" + f.getPrevCode() + ",&" + ((SubEnv)env).getToPointerVarName(i);
               f.setPrevCode(c);
                return field.compile(writer,env,it,id.getLine(),id.getTokenNum());
            }
            return field.compile(writer,env,it,id.getLine(),id.getTokenNum());
        }
        writer.write(b.getCode());
		return returnType;
	}
    @Override
    public final void setPrevCode(String code){
        prevCode = code;
    }
    @Override
    public final void writePrev(BufferedWriter writer)throws IOException{
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
    public final String getPrevCode(){
        return prevCode;
    }
    public final TypeInfo getReturnType(){
        return returnType;
    }
    public final FunctionType getType(){
        return type;
    }
    public final boolean isVirtual(){
        return type == FunctionType.ABSTRACT || type == FunctionType.VIRTUAL;
    }
    public final TypeInfo getSelf(){
        return self;
    }
    public final boolean hasSignature(Function f){
        if(returnType.equals(f.returnType)){
            for(var v:variants){
                if(f.hasVariant(v.args())){
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public final boolean equals(Object o){
        if(o instanceof Function f){
            return hasSignature(f);
        }
        return false;
    }
    public final Function makeMethod(TypeInfo ofType, String newName){
        var variant = getPointerVariant();
        var args = new TypeInfo[variant.args().length - 1];
        if(args.length > 0){
            System.arraycopy(variant.args(),1,args,0,args.length);
        }
        var f = new Function(newName,returnType,type,ofType,accessModifier);
        f.variants.add(new FunctionVariant(args,variant.cname(),variant.implName()));
        return f;
    }
    public final Function changeAccessModifier(AccessModifier accessModifier){
        var f = new Function(getName(),returnType,type,self,accessModifier);
        f.variants.addAll(variants);
        f.declaration.append(declaration);
        return f;
    }
    public final FunctionVariant addVariant(TypeInfo[]args,String cname,String implName){
        if(type == FunctionType.NATIVE){
            declaration.append("extern ");
        } else if(accessModifier == AccessModifier.PRIVATE){
            declaration.append("static ");
        }
        FunctionInfo f = null;
        var returnType = this.returnType;
        if(returnType instanceof IPointerInfo p){
            f = p.getFunctionPointer();
            if(f != null){
                returnType = f.getFunction().returnType;
            }
        }
        declaration.append(returnType.getCname()).append(' ');
        if(f != null){
            var times = ((IPointerInfo)this.returnType).getFunctionPointerDepth() + 1;
            declaration.append('(').append("*".repeat(Math.max(0,times)));
        }
        if(attrCode != null){
            declaration.append(attrCode).append(' ');
        }
        if(type != FunctionType.NATIVE){
            cname = cname + variants.size();
            implName = implName + variants.size();
        }
        declaration.append(cname).append('(');
        var first = self == null;
        if(self != null){
            declaration.append(self.getCname()).append("* this");
        }
        for(var arg:args){
            if(first){
                first = false;
            }else{
                declaration.append(',');
            }
            declaration.append(arg.getCname());
        }
        declaration.append(')');
        if(f != null){
            declaration.append(")(");
            for(var arg:f.getFunction().getPointerVariant().args()){
                declaration.append(arg.getCname());
            }
            declaration.append(')');
        }
        declaration.append(";\n");
        var v = new FunctionVariant(args,cname,implName);
        variants.add(v);
        return v;
    }
    public final String getName(){
        return name;
    }
    public final String getDeclaration(){
        return declaration.toString();
    }
    public final FunctionVariant getVariant(TypeInfo[]args){
        for(var v:variants){
            if(Arrays.equals(v.args(),args)){
                return v;
            }
        }
        return null;
    }
    public final ArrayList<TypeInfo>getRequiredTypes(){
        var list = new ArrayList<TypeInfo>();
        list.add(returnType);
        for(var v:variants){
            list.addAll(Arrays.stream(v.args()).toList());
        }
        return list;
    }
    public final boolean notFunctionPointer(){
        return variants.size() != 1;
    }
    public final FunctionVariant getPointerVariant(){
        return variants.get(0);
    }
    public final ArrayList<FunctionVariant>getVariants(){
        return variants;
    }
    public final void addStaticVariant(TypeInfo[]args,String cname){
        addVariant(args,cname,cname);
    }
    public final boolean hasVariant(TypeInfo[]args){
        for(var v:variants){
           if(Arrays.equals(v.args(),args)){
               return true;
           }
        }
        return false;
    }
    public final void calledOnReturnedInstance(){
        callStatus = CALLED_ON_RETURNED_INSTANCE;
    }
    public final void prepareForDereference(){
        asFunctionPointer = true;
    }
    @Override
    public final void calledOnPointer(){
        callStatus = CALLED_ON_POINTER;
    }
    public final void addTemplateVariant(LinkedHashMap<String,TypeInfo>templateArgs,AExp code,LinkedHashMap<String,TypeInfo>args,AEnv env){
        templateVariants.add(new TemplateVariant(templateArgs,code,args,env));
    }
    public final Function makeMethodFromTemplate(TypeInfo self,TypeInfo[]args,AEnv env){
        var f = new Function(name,returnType,FunctionType.NORMAL,self,accessModifier);
        var array = new TypeInfo[args.length + 1];
        array[0] = self;
        for(int i = 1,j = 0;i < array.length;++i,++j){
            array[i] = args[j];
        }
        var v = getTemplateVariant(array);
        if(v == null){
            throw new IllegalStateException("internal error:template not found " + Arrays.toString(args));
        }
        f.addVariantFromTemplate(v,env,array,true);
        return f;
    }
    private TemplateVariant getTemplateVariant(TypeInfo[]args){
        for(var v:templateVariants){
           var vArgs  = new ArrayList<>(v.args.values());
           if(args.length == v.args.size()){
               var found = true;
               for(int i = 0; i < v.args.size();++i){
                   var arg = args[i];
                   if(arg instanceof PointerInfo p){
                       arg = p.getType();
                   }
                   var variantArg = vArgs.get(i);
                   if(variantArg instanceof PointerInfo p){
                       variantArg = p.getType();
                   }
                   if(!v.templateArgs.containsKey(variantArg.getName()) && !variantArg.equals(arg)){
                       found = false;
                       break;
                   }
               }
               if(found){
                   return v;
               }
           }
        }
        return null;
    }
    private void addVariantFromTemplate(TemplateVariant variant,AEnv env,TypeInfo[]argsForTemplate,boolean asMethod){
        var cname = IFunction.createTemplateTypeCname(IFunction.toCId(name),argsForTemplate);
       try(var writer = Files.newBufferedWriter(Paths.get(env.getFPL().getOutputDirectory() + "/" + cname + ".c"))){
           var fnEnv = new FnEnv(variant.superEnv,returnType);
           var len = variant.args.size();
           if(asMethod){
               len--;
           }
           var args = new TypeInfo[len];
           var argsI = 0;
           var first = true;
           for(var entry:variant.args.entrySet()){
               var type = entry.getValue();
               var pointerDepth = 0;
               while(type instanceof PointerInfo p){
                   type = p.getType();
                   pointerDepth++;
               }
               if(variant.templateArgs.containsKey(type.getName())){
                   var argNum = 0;
                   for(var key:variant.templateArgs.keySet()){
                       if(key.equals(type.getName())){
                           break;
                       }
                       argNum++;
                   }
                   type = argsForTemplate[argNum];
                   for(int i = 0;i < pointerDepth;++i){
                       type = new PointerInfo(type);
                   }

               }
               if(asMethod){
                   if(first){
                       first = false;
                   }else{
                       args[argsI] = type;
                       argsI++;
                   }
               }else{
                   args[argsI] = type;
                   argsI++;
               }
               fnEnv.addFunction(entry.getKey(),new Variable(type,IFunction.toCId(entry.getKey()),entry.getKey()));
           }
           variant.code.compile(writer,fnEnv,null);
           addVariant(args,cname,cname);
       }catch(IOException e){
           throw new UncheckedIOException(e);
       }catch(CompilerException e){
           throw new RuntimeException(e);
       }
    }
    private record ReturnedData(String code,boolean notReturnedByFunction){}
    private record TemplateVariant(LinkedHashMap<String,TypeInfo>templateArgs,AExp code,LinkedHashMap<String,TypeInfo>args,
                                   AEnv superEnv){}
}