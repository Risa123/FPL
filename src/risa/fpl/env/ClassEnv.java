package risa.fpl.env;


import risa.fpl.BuilderWriter;
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
import java.util.HashMap;


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
               }else if(v.getType().isPrimitive() && !(v.getType() instanceof IPointerInfo)){
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
	    var additionalCode = "";
        var primaryParent = (InstanceInfo) instanceInfo.getPrimaryParent();
        //check for implicit constructor
        if(primaryParent != null && primaryParent.getConstructor().hasVariant(new TypeInfo[0])){
            additionalCode += primaryParent.getConstructor().getVariant(new TypeInfo[0]).cname() + "((" + primaryParent.getCname() + "*)this);\n";
        }
	    if(!isAbstract() && !struct){
	        additionalCode += "this->objectData=&" + instanceInfo.getDataName() + ";\n";
	     }
		return additionalCode + implicitConstructor;
	}
	public String getImplicitConstructor(){
        var header = IFunction.INTERNAL_METHOD_PREFIX + nameSpace + "_init0(";
        if(struct){
            return "#define " + header + "this) //placeholder\n";//structs have no implicit constructor
        }
        return "void " + header + instanceInfo.getCname() + "* this){\n" + getImplicitConstructorCode() + "}\n";
    }
    public void addMethod(Function method,String code){
         if(method.getAccessModifier() == AccessModifier.PRIVATE && !hasModifier(Modifier.NATIVE)){
              appendFunctionCode("static ");
         }
        appendFunctionCode(code);
    }
    public void addConstructor(String code,HashMap<String,TypeInfo>args,TypeInfo[]argsArray){
        var constructor = instanceInfo.getConstructor();
        if(onlyImplicitConstructor){
            onlyImplicitConstructor = false;
            constructor.getVariants().clear();
            ((Function)instanceInfo.getClassInfo().getFieldFromThisType("new")).getVariants().clear();
            ((Function)instanceInfo.getClassInfo().getFieldFromThisType("alloc")).getVariants().clear();
        }
        var argsCode = new StringBuilder();
        for(var entry:args.entrySet()){
            argsCode.append(',').append(entry.getValue().getCname()).append(' ').append(IFunction.toCId(entry.getKey()));
        }
        constructors.add(new ConstructorData(code,constructor.addVariant(argsArray,nameSpace).cname(),argsCode.toString()));
        var b = new BuilderWriter();
        compileNewAndAlloc(b,argsArray);
        appendFunctionCode(b.getCode());
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
    public void compileNewAndAlloc(BuilderWriter writer,TypeInfo[]args){
        var constructorName = instanceInfo.getConstructor().getVariant(args).cname();
        var cname = instanceInfo.getCname();
        writer.write("void " + constructorName + "(" + cname + "* this");
        for(var arg:args){
            writer.write("," + arg.getCname());
        }
        var classInfo = instanceInfo.getClassInfo();
        writer.write(");\n");
        var allocName = "static" + getNameSpace() + "_alloc";
        var allocMethod = (Function)classInfo.getFieldFromThisType("alloc");
        allocMethod.addStaticVariant(args,allocName);
        writer.write(instanceInfo.getCname() + "* " + allocMethod.getVariant(args).cname() + "(");
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
        writer.write(compiledArgs + "){\n");
        writer.write("void* malloc(" + NumberInfo.MEMORY.getCname() + ");\n");
        writer.write(cname + "* p=malloc(sizeof(" + cname + "));\n");
        writer.write(constructorCall(constructorName,"p",args));
        writer.write("return p;\n}\n");
        var newName = "static" + nameSpace + "_new";
        var newMethod = (Function)classInfo.getFieldFromThisType("new");
        newMethod.addStaticVariant(args,newName);
        writer.write(cname + " " + newMethod.getVariant(args).cname() + "(" + compiledArgs + "){\n");
        writer.write(cname + " inst;\n");
        writer.write(constructorCall(constructorName,"&inst",args));
        writer.write("return inst;\n}\n");
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
            b.append("void ").append(data.cname()).append('(').append(instanceInfo.getCname()).append("* this");
            b.append(data.argsCode()).append("){\n");
            b.append(getImplicitConstructorCode());
            b.append(data.code()).append("}\n");
        }
        return b.toString();
    }
    private record ConstructorData(String code,String cname,String argsCode){}
}