package risa.fpl.function.exp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;

import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.info.*;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

public class Function extends AField implements ICalledOnPointer{
	private final TypeInfo self,returnType;
	private final StringBuilder declaration = new StringBuilder();
	private final FunctionType type;
	private int callStatus;
	private boolean asFunctionPointer;
	public static final int NO_STATUS = 0,CALLED_ON_POINTER = 1,CALLED_ON_RETURNED_INSTANCE = 2,CALLED_ON_INSTANCE_R_BY_FUNC = 3;
	private final String name,attrCode;
	private final ArrayList<FunctionVariant>variants = new ArrayList<>();
	private final ArrayList<TemplateVariant>templateVariants = new ArrayList<>();
    public Function(String name,TypeInfo returnType,FunctionType type,TypeInfo self,AccessModifier accessModifier,String attrCode){
       super(accessModifier);
       this.returnType = returnType;
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
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        var b = new StringBuilder();
		var argList = new ArrayList<TypeInfo>();
		var returnedData = new ArrayList<ReturnedData>();
		if(self instanceof InstanceInfo i && i.isException() && name.equals("throw") && env instanceof FnSubEnv subEnv){
            subEnv.getReturnType();//to prevent no return error
        }
		while(it.hasNext() && !(it.peek() instanceof List)){
		   var exp = it.nextAtom();
		   if(exp.getType() == AtomType.END_ARGS){
			   break;
		   }else if(exp.getType() != AtomType.ARG_SEPARATOR){
			   var buffer = new StringBuilder();
			   var f = env.getFunction(exp);
               argList.add(f.compile(buffer,env,it,exp.getLine(),exp.getTokenNum()));
               returnedData.add(new ReturnedData(buffer.toString(),!(f instanceof  Function)));
		   }
		}
		var array = argList.toArray(new TypeInfo[0]);
		if(!hasVariant(array)){
		    throw new CompilerException(line,tokenNum,"function has no variant with arguments " + Arrays.toString(array));
        }
		var variant = getVariant(array);
        if(isVirtual()){
            if(self instanceof InstanceInfo i){
                b.append("((").append(i.getClassDataType()).append("*)");
            }
            b.append(Objects.requireNonNullElse(prevCode,"this"));
            if(self instanceof InterfaceInfo){
                b.append(".impl->");
            }else{
                if(callStatus == CALLED_ON_POINTER || prevCode == null){
                    b.append("->");
                }else{
                    b.append('.');
                }
                b.append("objectData)->");
            }
        }
        if(asFunctionPointer){
            asFunctionPointer = false;
        }else{
            //noinspection ConstantConditions
            b.append(variant.implName());
        }
        b.append('(');
		var first = self == null;
		var ref = false;
        var noPrevCode = prevCode == null;
		if(self != null){
            if(!self.isPrimitive()){
                b.append('(').append(self.getCname()).append("*)");
            }
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
                       b.append("(&");
                   }
               }
            }
            writePrev(b);
		    if(ref){
                b.append(')');
            }
		    if(callStatus == CALLED_ON_INSTANCE_R_BY_FUNC){
		        b.append(')');
		        callStatus = NO_STATUS;
            }
            if(self instanceof InterfaceInfo){
                b.append(".instance");
            }
        }
        for(int i = 0;i < array.length;++i){
            if(first){
                first = false;
            }else{
                b.append(',');
            }
            var comesFromPointer = array[i] instanceof PointerInfo;
            //noinspection ConstantConditions
            b.append(array[i].ensureCast(variant.args()[i],returnedData.get(i).code,comesFromPointer,returnedData.get(i).notReturnedByFunction));
        }
		b.append(')');
        if(it.hasNext() && returnType != TypeInfo.VOID && it.peek() instanceof Atom a){
           if(a.getType() == AtomType.ID){
               var id = it.nextID();
               var field = returnType.getField(id.getValue(),env);
               if(field == null){
                   throw new CompilerException(id,returnType + " has no field called " + id);
               }
               field.setPrevCode(b.toString());
               if(returnType instanceof InstanceInfo i){
                   if(field instanceof Function f){
                       f.callStatus = CALLED_ON_INSTANCE_R_BY_FUNC;
                   }
                   field.setPrevCode(i.getToPointerName() + '(' + field.getPrevCode() + ",&" + env.getToPointerVarName(i));
                   return field.compile(builder,env,it,id.getLine(),id.getTokenNum());
               }
               return field.compile(builder,env,it,id.getLine(),id.getTokenNum());
           }else if(a.getType() == AtomType.END_ARGS && noPrevCode){
               throw new CompilerException(a,"; not expected");
           }
        }
        builder.append(b);
		return returnType;
	}
    @Override
    public final void setPrevCode(String code){
        prevCode = code;
    }
    public final void writePrev(StringBuilder builder){
        if(prevCode == null){
           if(self != null){
               builder.append("this");
           }
        }else{
            builder.append(prevCode);
            prevCode = null;
        }
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
    public final Function makeMethod(TypeInfo ofType,String newName){
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
        var f = new Function(name,returnType,type,self,accessModifier);
        f.variants.addAll(variants);
        f.declaration.append(declaration);
        return f;
    }
    public final FunctionVariant addVariant(TypeInfo[]args,String cname,String implName){
        if(type == FunctionType.NATIVE){
            declaration.append("extern ");
        }else if(accessModifier == AccessModifier.PRIVATE){
            declaration.append("static ");
        }
        var returnType = this.returnType;
        declaration.append(returnType.getCname()).append(' ');
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
            list.addAll(Arrays.asList(v.args()));
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
    public final Function makeMethodFromTemplate(TypeInfo self,TypeInfo[]args,ModuleEnv module){
        var f = new Function(name,returnType,FunctionType.NORMAL,self,accessModifier);
        var array = new TypeInfo[args.length + 1];
        array[0] = self;
        System.arraycopy(args,0,array,1,args.length);
        var v = getTemplateVariant(array);
        if(v == null){
            throw new IllegalStateException("internal error:template not found " + Arrays.toString(args));
        }
        f.addVariantFromTemplate(v,module,array,true);
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
    private void addVariantFromTemplate(TemplateVariant variant,SubEnv env,TypeInfo[]argsForTemplate,boolean asMethod){
        var mod = env.getModule();
        var cname = mod.getNameSpace() + IFunction.createTemplateTypeCname(IFunction.toCId(name),argsForTemplate);
        var path = Path.of(env.getFPL().getOutputDirectory() + '/' + cname + ".c");
        var builder = new StringBuilder();
        try{
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
           var v = addVariant(args,cname,cname);
           builder.append(returnType.getCname()).append(' ').append(v.cname()).append('(');
           var firstArg = true;
           if(self != null){
               builder.append(self.getCname());
           }
           for(var arg:variant.args.entrySet()){
               if(firstArg){
                   firstArg = false;
               }else{
                   builder.append(',');
               }
               var type = arg.getValue();
               while(type instanceof PointerInfo p){
                   type = p.getType();
               }
               var typeName = type.getName();
               if(variant.templateArgs.containsKey(typeName)){
                  type = variant.templateArgs.get(typeName);
               }
               builder.append(type.getCname()).append(' ').append(IFunction.toCId(arg.getKey()));
           }
           builder.append("){\n");
           variant.code.compile(builder,fnEnv,null);
           builder.append('}');
           var selfType = self;
           if(selfType instanceof PointerInfo p){
               selfType = p.getType();
           }
           if(!(selfType instanceof InstanceInfo i) || i.canWriteTemplateFunctionVariants()){
               mod.getFPL().addFunctionVariantGenerationData(new VariantGenData(builder.toString(),path,mod));
           }
       }catch(CompilerException e){
           throw new RuntimeException(e);
       }
    }
    private record ReturnedData(String code,boolean notReturnedByFunction){}
    private record TemplateVariant(LinkedHashMap<String,TypeInfo>templateArgs,AExp code,LinkedHashMap<String,TypeInfo>args,
                                   AEnv superEnv){}
}