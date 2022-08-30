package risa.fpl.env;


import risa.fpl.CompilerException;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.AddModifier;
import risa.fpl.function.SetAccessModifier;
import risa.fpl.function.block.*;
import risa.fpl.function.exp.*;
import risa.fpl.function.statement.Var;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;

import java.util.ArrayList;
import java.util.Objects;


public final class ClassEnv extends ANameSpacedEnv implements IClassOwnedEnv{
	private final StringBuilder implicitConstructor = new StringBuilder();
	private final InstanceInfo instanceInfo;
	private final StringBuilder implCopyConstructorCode = new StringBuilder(),defaultCopyConstructorCode = new StringBuilder();
    private final boolean struct;
	private boolean parentConstructorCalled,destructorDeclared,copyConstructorDeclared;
    private ArrayList<ExpressionInfo>block;
	private static final SetAccessModifier PROTECTED = new SetAccessModifier(AccessModifier.PROTECTED);
	private static final SetAccessModifier INTERNAL = new SetAccessModifier(AccessModifier.INTERNAL);
	private static final AddModifier VIRTUAL = new AddModifier(Modifier.VIRTUAL);
	private static final AddModifier OVERRIDE = new AddModifier(Modifier.OVERRIDE);
	private static final Destructor DESTRUCTOR = new Destructor();
	private static final Constructor CONSTRUCTOR = new Constructor();
	private static final CopyConstructor COPY_CONSTRUCTOR = new CopyConstructor();
    private boolean onlyImplicitConstructor = true;
    private final ArrayList<ConstructorData>constructors = new ArrayList<>();
    private final ArrayList<String>variableFieldDeclarationOrder = new ArrayList<>();
    private final int firstLine;
	public ClassEnv(ModuleEnv module,String id,TemplateStatus templateStatus,boolean struct,int firstLine){
		super(module,module.getNameSpace() + (templateStatus == TemplateStatus.GENERATING?"":IFunction.toCId(id)));
		super.addFunction("this",CONSTRUCTOR);
		super.addFunction("protected",PROTECTED);
		if(!struct){
            super.addFunction("virtual",VIRTUAL);
            super.addFunction("override",OVERRIDE);
        }
		super.addFunction("internal",INTERNAL);
		super.addFunction("-this",DESTRUCTOR);
		super.addFunction("=this",COPY_CONSTRUCTOR);
        this.struct = struct;
        this.firstLine = firstLine;
        if(templateStatus == TemplateStatus.TEMPLATE){
            instanceInfo = new TemplateTypeInfo(id,module,nameSpace,((SubEnv)superEnv).hasModifier(Modifier.FINAL));
        }else{
            instanceInfo = new InstanceInfo(id,module,nameSpace,((SubEnv)superEnv).hasModifier(Modifier.FINAL));
            instanceInfo.setClassEnv(this);
        }
        var prefix = "static" + nameSpace;
        //checking if not generating from template to prevent generated type from displacing the template
        if(templateStatus != TemplateStatus.GENERATING){
            module.addType(instanceInfo);
        }
        ((Function)instanceInfo.getClassInfo().getFieldFromThisType("alloc")).getVariants().add(new FunctionVariant(new TypeInfo[0],FunctionType.NORMAL,prefix + "_alloc0",prefix + "_alloc0",null));
        ((Function)instanceInfo.getClassInfo().getFieldFromThisType("new")).getVariants().add(new FunctionVariant(new TypeInfo[0],FunctionType.NORMAL,prefix + "_new0",prefix + "_new0",null));
        var name = IFunction.INTERNAL_PREFIX + nameSpace + "_init0";
        instanceInfo.getConstructor().getVariants().add(new FunctionVariant(new TypeInfo[0],FunctionType.NORMAL,name,name,null));
	}
	@Override
	public void addFunction(String name,IFunction value){
		if(value instanceof AField field){
			instanceInfo.addField(name,field);
			if(value instanceof Variable v){
			   var cname = v.getCname();
			   if(v.getType() instanceof InstanceInfo i){
                   var destructor = i.getDestructorName();
                   var copyName = i.getCopyConstructorName();
                   if(destructor != null){
                       appendToDestructor(destructor + "(&this->" + cname +");\n");
                   }
                   if(copyName != null){
                       implCopyConstructorCode.append(copyName).append("(&this->").append(cname).append(",&o->").append(cname).append(");\n");
                   }
               }else if(v.getType().isPrimitive() && !(v.getType() instanceof PointerInfo)){
			       defaultCopyConstructorCode.append("this->").append(cname).append("=o->").append(cname).append(";\n");
               }else if(v.getType() instanceof InterfaceInfo i){
                    appendToDestructor(i.getDestructorName() + "(&this->" + cname + ");\n");
                    implCopyConstructorCode.append(i.getCopyName()).append("(&this->").append(cname).append(",&o->").append(cname).append(");\n");
               }
            }
        }else{
		    super.addFunction(name,value);
        }
	}
	public void appendToImplicitConstructor(String code){
		implicitConstructor.append(code);
	}
	public String getImplicitConstructorCode(){
		return implicitConstructor.toString();
	}
	public String getImplicitConstructor(Atom classId)throws CompilerException{
        if(struct){
            return "";
        }
        var header = IFunction.INTERNAL_PREFIX + nameSpace + "_init0(";
        var code = "this->objectData=&" + instanceInfo.getDataName() + ";\n";
        if(instanceInfo.getPrimaryParent() != null){
            var parent = (InstanceInfo)instanceInfo.getPrimaryParent();
            var constructor = parent.getConstructor();
            if(!constructor.hasVariant(new TypeInfo[0])){
                error(classId,"this class can not have implicit constructor if parent has not one");
            }
            code = constructor.getVariant(new TypeInfo[0]).getCname() + "((" + parent.getCname() + "*)this);\n" + code;
        }
        return "void " + header + instanceInfo.getCname() + "* this){\n" + code + getImplicitConstructorCode() + "}\n";
    }
    public void compileMethodVariant(FunctionVariant variant,Function method,String code){
        if(variant.getType() != FunctionType.ABSTRACT){
            if(method.getAccessModifier() == AccessModifier.PRIVATE && variant.getType() != FunctionType.NATIVE){
                appendFunctionCode("static ");
            }
            appendFunctionCode(code);
        }
    }
    public void addConstructor(String code,String argsCode,TypeInfo[]args,String parentConstructorCall,int line){
        var constructor = instanceInfo.getConstructor();
        var v = constructor.hasVariant(args)?constructor.getVariant(args):constructor.addVariant(args,nameSpace);
        v.setLine(line);
        constructors.add(new ConstructorData(code,v.getCname(),argsCode,parentConstructorCall));
        var b = new StringBuilder("void ");
        var constructorName = Objects.requireNonNull(instanceInfo.getConstructor().getVariant(args)).getCname();
        var cname = instanceInfo.getCname();
        b.append(constructorName).append('(').append(cname).append("* this");
        for(var arg:args){
            b.append(',').append(arg.getCname());
        }
        b.append(");\n");
        var classInfo = instanceInfo.getClassInfo();
        var allocName = "static" + getNameSpace() + "_alloc";
        var allocMethod = (Function)classInfo.getFieldFromThisType("alloc");
        allocMethod.addStaticVariant(args,allocName);
        b.append(instanceInfo.getCname()).append("* ").append(allocMethod.getVariant(args).getCname()).append('(');
        var first = true;
        var b2 = new StringBuilder();
        for(int i = 0; i < args.length;++i){
            if(first){
                first = false;
            }else{
                b2.append(',');
            }
            b2.append(args[i].getCname()).append(" a").append(i);
        }
        var compiledArgs = b2.toString();
        b.append(compiledArgs).append("){\n");
        b.append("void* malloc(").append(NumberInfo.MEMORY.getCname()).append(");\n");
        b.append(cname).append("* p=malloc(sizeof(").append(cname).append("));\n");
        b.append(constructorCall(constructorName,"p",args));
        b.append("return p;\n}\n");
        var newName = "static" + nameSpace + "_new";
        var newMethod = (Function)classInfo.getFieldFromThisType("new");
        newMethod.addStaticVariant(args,newName);
        b.append(cname).append(' ').append(newMethod.getVariant(args).getCname()).append('(').append(compiledArgs).append("){\n");
        b.append(cname).append(" inst;\n");
        b.append(constructorCall(constructorName,"&inst",args));
        b.append("return inst;\n}\n");
        if(instanceInfo.isException()){
            if(instanceInfo.getClassInfo().getFieldFromThisType("throw") == null){
                instanceInfo.getClassInfo().addField("throw",new StaticThrow());
            }
            var func = (Function)classInfo.getFieldFromThisType("throw");
            func.addStaticVariant(args,"static" + nameSpace + "_throw");
            b.append("void ").append(func.getVariant(args).getCname()).append('(');
            var argFirst = true;
            for(var i = 0;i < args.length;++i){
                if(argFirst){
                    argFirst = false;
                }else{
                    b.append(',');
                }
                b.append(args[i].getCname()).append(" a").append(i);
            }
            b.append("){\n");
            b.append(instanceInfo.getCname()).append(" inst;\n");
            b.append(constructorName).append("(&inst");
            for(var i = 0;i < args.length;++i){
                b.append(",a").append(i);
            }
            b.append(");\n_std_lang_Exception_throw0((_Exception*)&inst);\n}\n");
        }
        appendFunctionCode(b.toString());
    }
    @Override
    public ClassInfo getClassInfo(){
	    return instanceInfo.getClassInfo();
    }
    public InstanceInfo getInstanceInfo(){
	    return instanceInfo;
    }
    @Override
    public String getNameSpace(IFunction caller){
	    return caller instanceof Var || caller instanceof Function f && f.getAccessModifier() == AccessModifier.PRIVATE?"":nameSpace;
    }
    @Override
    public void addTemplateInstance(InstanceInfo type){
        ((ModuleEnv)superEnv).addTemplateInstance(type);
    }
    @Override
    public IFunction getFunction(Atom name)throws CompilerException{
	    var field = instanceInfo.getField(name.getValue(),this);
	    return field != null?field:super.getFunction(name);
    }
    public boolean notAbstract(){
	    return !((ModuleEnv)superEnv).hasModifier(Modifier.ABSTRACT);
    }
    public String getImplOf(InterfaceInfo i){
	    return instanceInfo.getCname() + i.getCname() + "_impl";
    }
    public String getDataDefinition(){
        if(struct){
            return "";
        }
        var b = new StringBuilder(instanceInfo.getClassDataType());
        b.append(' ').append(instanceInfo.getDataName()).append("={sizeof(").append(instanceInfo.getCname()).append(')');
        if(notAbstract()){
          for(var entry:instanceInfo.getMethodVariantsOfType(FunctionType.VIRTUAL).entrySet()){
              b.append(",(void*)").append(entry.getKey().getCname());
          }
        }
        return b.append("};\n").toString();
    }
    public void parentConstructorCalled(){
	    parentConstructorCalled = true;
    }
    public boolean isParentConstructorCalled(){
	    return parentConstructorCalled;
    }
    public void destructorDeclared(){
	    destructorDeclared = true;
    }
    public boolean isDestructorDeclared(){
	    return destructorDeclared;
    }
    public String getDestructor(){
	    if(!(destructor.isEmpty() || destructorDeclared)){
            var b = new StringBuilder("void ");
            var prefix = IFunction.INTERNAL_PREFIX + nameSpace;
            instanceInfo.setDestructorName(prefix);
            b.append(prefix).append("_destructor(").append(instanceInfo.getCname());
            return b.append("* this){\n").append(destructor).append("}\n").toString();
        }
	    return "";
    }
    public String getImplicitDestructorCode(){
	    return destructor.toString();
    }
    private String constructorCall(String constructorName,String self,TypeInfo[]args){
        var b = new StringBuilder(constructorName).append('(').append(self);
        for(int i = 0; i < args.length;++i){
            b.append(",a").append(i);
        }
        return b.append(");\n").toString();
    }
    public String getInitializer(){
	    return getInitializer("cinit") + '\n';
    }
    public boolean isCopyConstructorDeclared(){
	    return copyConstructorDeclared;
    }
    public void declareCopyConstructor(){
	    copyConstructorDeclared = true;
    }
    public String getImplicitCopyConstructorCode(){
	    return implCopyConstructorCode.toString();
    }
    public String getDefaultCopyConstructorCode(){
	    return defaultCopyConstructorCode.toString();
    }
    public void setBlock(ArrayList<ExpressionInfo>block){
        this.block = block;
    }
    public ArrayList<ExpressionInfo>getBlock(){
        return block;
    }
    public void setTypeForTemplateArgument(String name,TypeInfo type){
        types.put(name,type);
        addFunction(name,new Var(type));
    }
    public boolean notStruct(){
        return !struct;
    }
    public boolean hasOnlyImplicitConstructor(){
        return onlyImplicitConstructor;
    }
    @Override
    public boolean hasFunctionInCurrentEnv(String name){
        return instanceInfo.getFieldFromThisType(name) != null || super.hasFunctionInCurrentEnv(name);
    }
    public void removeImplicitConstructor(){
        if(onlyImplicitConstructor){
            onlyImplicitConstructor = false;
            instanceInfo.getConstructor().getVariants().clear();
            ((Function)instanceInfo.getClassInfo().getFieldFromThisType("new")).getVariants().clear();
            ((Function)instanceInfo.getClassInfo().getFieldFromThisType("alloc")).getVariants().clear();
        }
    }
    public String getConstructorCode(){
        var b = new StringBuilder();
        for(var data:constructors){
            b.append("void ").append(data.cname).append(data.argsCode()).append("{\n");
            b.append(data.parentConstructorCall);
            b.append("this->objectData=&").append(instanceInfo.getDataName()).append(";\n");
            b.append(getImplicitConstructorCode());
            b.append(data.code);
            b.append("}\n");
        }
        return b.toString();
    }
    public ArrayList<String>getVariableFieldDeclarationOrder(){
        return variableFieldDeclarationOrder;
    }
    public int getFirstLine(){
        return firstLine;
    }
    private record ConstructorData(String code,String cname,String argsCode,String parentConstructorCall){}
}