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


public final class ClassEnv extends ANameSpacedEnv implements IClassOwnedEnv{
	private final StringBuilder implicitConstructor = new StringBuilder();
	private final String nameSpace;
	private final InstanceInfo instanceInfo;
	private final StringBuilder implCopyConstructorCode = new StringBuilder();
	private final StringBuilder defaultCopyConstructorCode = new StringBuilder();
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
	public ClassEnv(ModuleEnv module,String id,TemplateStatus templateStatus,boolean struct){
		super(module);
		super.addFunction("this",CONSTRUCTOR);
		super.addFunction("protected",PROTECTED);
		if(!struct){
            super.addFunction("virtual",VIRTUAL);
            super.addFunction("override",OVERRIDE);
        }
		super.addFunction("internal",INTERNAL);
		super.addFunction("-this",DESTRUCTOR);
		super.addFunction("=this",COPY_CONSTRUCTOR);
		var cname = IFunction.toCId(id);
		if(templateStatus == TemplateStatus.GENERATING){
		    nameSpace = module.getNameSpace();
        }else{
            nameSpace = module.getNameSpace() + cname;
        }
        this.struct = struct;
        if(templateStatus == TemplateStatus.TEMPLATE){
            instanceInfo = new TemplateTypeInfo(id,module,nameSpace);
        }else{
            instanceInfo = new InstanceInfo(id,module,nameSpace);
            instanceInfo.setClassEnv(this);
        }
        var prefix = "static" + nameSpace;
        //checking if not generating from template to prevent generated type from displacing the template
        if(templateStatus != TemplateStatus.GENERATING){
            module.addType(instanceInfo);
        }
        //setup implicit constructor,alloc and new
        ((Function)instanceInfo.getClassInfo().getFieldFromThisType("alloc")).getVariants().add(new FunctionVariant(new TypeInfo[0],prefix + "_alloc0",prefix + "_alloc0"));
        ((Function)instanceInfo.getClassInfo().getFieldFromThisType("new")).getVariants().add(new FunctionVariant(new TypeInfo[0],prefix + "_new0",prefix + "_new0"));
        var name = IFunction.INTERNAL_METHOD_PREFIX + nameSpace + "_init0";
        instanceInfo.getConstructor().getVariants().add(new FunctionVariant(new TypeInfo[0],name,name));
	}
	@Override
	public void addFunction(String name,IFunction value){
		if(value instanceof IField field){
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
                       implCopyConstructorCode.append(copyName).append("(&this->").append(cname);
                       implCopyConstructorCode.append(",&o->").append(v.getCname()).append(");\n");
                   }
               }else if(v.getType().isPrimitive() && !(v.getType() instanceof PointerInfo)){
			       defaultCopyConstructorCode.append("this->").append(cname).append("=o->").append(cname).append(";\n");
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
	public String getImplicitConstructor(){
        var header = IFunction.INTERNAL_METHOD_PREFIX + nameSpace + "_init0(";
        if(struct){
            return "#define " + header + "this) //placeholder\n";//structs have no implicit constructor
        }
        var setObjectData = "this->objectData=&" + instanceInfo.getDataName() + ";\n";
        return "void " + header + instanceInfo.getCname() + "* this){\n" + setObjectData + getImplicitConstructorCode() + "}\n";
    }
    public void addMethod(Function method,String code){
         if(method.getAccessModifier() == AccessModifier.PRIVATE && !hasModifier(Modifier.NATIVE)){
              appendFunctionCode("static ");
         }
        appendFunctionCode(code);
    }
    public void addConstructor(String code,String argsCode,TypeInfo[]argsArray,String parentConstructorCall){
        var constructor = instanceInfo.getConstructor();
        if(onlyImplicitConstructor){
            onlyImplicitConstructor = false;
            constructor.getVariants().clear();
            ((Function)instanceInfo.getClassInfo().getFieldFromThisType("new")).getVariants().clear();
            ((Function)instanceInfo.getClassInfo().getFieldFromThisType("alloc")).getVariants().clear();
        }
        constructors.add(new ConstructorData(code,constructor.addVariant(argsArray,nameSpace).cname(),argsCode,parentConstructorCall));
        var b = new StringBuilder();
        compileNewAndAlloc(b,argsArray);
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
	    if(caller instanceof Var || caller instanceof Function f && f.getAccessModifier() == AccessModifier.PRIVATE){
            return "";
        }
	    return nameSpace;
    }
    @Override
    public String getNameSpace(){
	    return nameSpace;
    }
    @Override
    public void addTemplateInstance(InstanceInfo type){
        ((ANameSpacedEnv)superEnv).addTemplateInstance(type);
    }
    @Override
    public IFunction getFunction(Atom name)throws CompilerException{
	    var field = instanceInfo.getField(name.getValue(),this);
	    if(field != null){
	        return field;
        }
	    return super.getFunction(name);
    }
    public boolean isAbstract(){
	    return ((SubEnv)superEnv).hasModifier(Modifier.ABSTRACT);
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
        if(!isAbstract()){
          for(var method: instanceInfo.getMethodsOfType(FunctionType.VIRTUAL)){
              for(var v:method.getVariants()){
                  b.append(",(void*)").append(v.cname());
              }
          }
        }
        b.append("};\n");
        return b.toString();
    }
    public void parentConstructorCalled(){
	    parentConstructorCalled = true;
    }
    public boolean isParentConstructorCalled(){
	    return parentConstructorCalled;
    }
    public void setPrimaryParent(InstanceInfo parent){
        instanceInfo.setPrimaryParent(parent);
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
            var prefix = IFunction.INTERNAL_METHOD_PREFIX + nameSpace;
            instanceInfo.setDestructorName(prefix);
            b.append(prefix).append("_destructor(").append(instanceInfo.getCname());
            b.append("* this){\n").append(destructor).append("}\n");
            return b.toString();
        }
	    return "";
    }
    public String getImplicitDestructorCode(){
	    return destructor.toString();
    }
    public void compileNewAndAlloc(StringBuilder builder,TypeInfo[]args){
        var constructorName = instanceInfo.getConstructor().getVariant(args).cname();
        var cname = instanceInfo.getCname();
        builder.append("void ").append(constructorName).append("(").append(cname).append("* this");
        for(var arg:args){
            builder.append(",").append(arg.getCname());
        }
        var classInfo = instanceInfo.getClassInfo();
        builder.append(");\n");
        var allocName = "static" + getNameSpace() + "_alloc";
        var allocMethod = (Function)classInfo.getFieldFromThisType("alloc");
        allocMethod.addStaticVariant(args,allocName);
        builder.append(instanceInfo.getCname()).append("* ").append(allocMethod.getVariant(args).cname()).append("(");
        var first = true;
        var b = new StringBuilder();
        for(int i = 0; i < args.length;++i){
            if(first){
                first = false;
            }else{
                b.append(',');
            }
            b.append(args[i].getCname()).append(" a").append(i);
        }
        var compiledArgs = b.toString();
        builder.append(compiledArgs).append("){\n");
        builder.append("void* malloc(").append(NumberInfo.MEMORY.getCname()).append(");\n");
        builder.append(cname).append("* p=malloc(sizeof(").append(cname).append("));\n");
        builder.append(constructorCall(constructorName,"p",args));
        builder.append("return p;\n}\n");
        var newName = "static" + nameSpace + "_new";
        var newMethod = (Function)classInfo.getFieldFromThisType("new");
        newMethod.addStaticVariant(args,newName);
        builder.append(cname).append(" ").append(newMethod.getVariant(args).cname()).append("(").append(compiledArgs).append("){\n");
        builder.append(cname).append(" inst;\n");
        builder.append(constructorCall(constructorName,"&inst",args));
        builder.append("return inst;\n}\n");
    }
    private String constructorCall(String constructorName,String self,TypeInfo[]args){
        var b = new StringBuilder(constructorName);
        b.append("(").append(self);
        for(int i = 0; i < args.length;++i){
            b.append(",a").append(i);
        }
        return b.append(");\n").toString();
    }
    public String getInitializer(){
	    return getInitializer("cinit");
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
    public boolean isStruct(){
        return struct;
    }
    public boolean hasOnlyImplicitConstructor(){
        return onlyImplicitConstructor;
    }
    public String getConstructorCode(){
        var b = new StringBuilder();
        for(var data:constructors){
            b.append("void ").append(data.cname);
            b.append(data.argsCode()).append("{\n");
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
    private record ConstructorData(String code,String cname,String argsCode,String parentConstructorCall){}
}