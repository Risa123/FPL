package risa.fpl.env;


import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.AddModifier;
import risa.fpl.function.SetAccessModifier;
import risa.fpl.function.block.*;
import risa.fpl.function.exp.*;
import risa.fpl.function.statement.InstanceVar;
import risa.fpl.function.statement.Var;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;

import java.util.ArrayList;


public final class ClassEnv extends ANameSpacedEnv implements IClassOwnedEnv{
	private final StringBuilder implicitConstructor = new StringBuilder();
	private final String nameSpace,dataType,dataName;
	private final ClassInfo classType;
	private final InstanceInfo instanceType;
	private final StringBuilder implBuilder = new StringBuilder();
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
        dataType = cname + "_data_type";
        dataName = nameSpace + "_data";
        classType = new ClassInfo(id,dataName);
        this.struct = struct;
        if(templateStatus == TemplateStatus.TEMPLATE){
            instanceType = new TemplateTypeInfo(id,module,nameSpace);
        }else{
            instanceType = new InstanceInfo(id,module,nameSpace);
        }
        classType.addField("alloc",new Function("alloc",new PointerInfo(instanceType),AccessModifier.PUBLIC));
        classType.addField("new",new Function("new",instanceType,AccessModifier.PUBLIC));
        instanceType.setClassInfo(classType);
        //checking if not generating from template to prevent generated type from displacing the template
        if(templateStatus != TemplateStatus.GENERATING){
            module.addType(instanceType);
        }
        instanceType.setConstructor(new InstanceVar(instanceType,classType));
        if(!struct){
            appendToInitializer(dataName + ".size=sizeof(" + cname +");\n");
        }
	}
	@Override
	public void addFunction(String name,IFunction value){
		if(value instanceof IField field){
			instanceType.addField(name,field);
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
        var primaryParent = (InstanceInfo)instanceType.getPrimaryParent();
        //check for implicit constructor
        if(primaryParent != null && primaryParent.getConstructor().hasVariant(new TypeInfo[0])){
            additionalCode += primaryParent.getConstructor().getVariant(new TypeInfo[0]).cname() + "((" + primaryParent.getCname() + "*)this);\n";
        }
	    if(!isAbstract() && !struct){
	        additionalCode += "this->objectData=&" + dataName + ";\n";
	     }
		return additionalCode + implicitConstructor;
	}
	public String getImplicitConstructor(){
        var header = IFunction.INTERNAL_METHOD_PREFIX +  nameSpace  + "_init0(";
        if(struct){
            return "#define " + header + "this) //struct without implicit constructor\n";
        }
        return "void " + header + instanceType.getCname() + "* this){\n" + getImplicitConstructorCode() + "}\n";
    }
    public void addMethod(Function method,TypeInfo[] args,String code){
         if(method.getAccessModifier() == AccessModifier.PRIVATE && !hasModifier(Modifier.NATIVE)){
              appendFunctionCode("static ");
         }
         if(method.getType() != FunctionType.ABSTRACT){
             appendFunctionCode(code);
         }
         if(method.isVirtual()){
         	 var parent = instanceType.getPrimaryParent();
         	 if(parent != null && parent.getField(method.getName(),this) instanceof Function){
         	 	return;
			 }
         	 var v = method.getVariant(args);
             implBuilder.append(new FunctionInfo(method).getPointerVariableDeclaration(v.implName())).append(";\n");
         }
    }
    @Override
    public ClassInfo getClassType(){
	    return classType;
    }
    public InstanceInfo getInstanceType(){
	    return instanceType;
    }
    @Override
    public String getNameSpace(IFunction caller){
	    if(caller instanceof Var || caller instanceof Function f && f.getAccessModifier() == AccessModifier.PRIVATE) {
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
    public void appendDeclarations(){
        for(var field:classType.getFields().values()){
            if(field instanceof Function f){
              appendFunctionDeclaration(f);
            }
        }
        for(var field:instanceType.getFields().values()){
            if(field instanceof Function f){
                if(f.getAccessModifier() != AccessModifier.PRIVATE){
                    appendFunctionDeclaration(f);
                }
            }
        }
        if(!(instanceType instanceof TemplateTypeInfo)){
            instanceType.setMethodDeclarations(getFunctionDeclarations());
        }
    }
    @Override
    public IFunction getFunction(Atom name)throws CompilerException{
	    var field = instanceType.getField(name.getValue(),this);
	    if(field != null){
	        return field;
        }
	    return super.getFunction(name);
    }
    public boolean isAbstract(){
	    return superEnv.hasModifier(Modifier.ABSTRACT);
    }
    public String getDataDeclaration(){
        if(struct){
            return "";
        }
        var b = new StringBuilder("typedef struct ");
        instanceType.setImplCode(implBuilder.toString());
        var primaryParent = (InstanceInfo)instanceType.getPrimaryParent();
        b.append(dataType).append("{\nunsigned long size;\n");
        if(primaryParent != null){
            b.append(primaryParent.getImplCode());
        }
        b.append(implBuilder).append('}').append(dataType).append(";\n");
        return b.toString();
    }
    public String getImplOf(InterfaceInfo i){
	    return instanceType.getCname() + i.getCname() + "_impl";
    }
    public String getDataDefinition(){
        if(struct){
            return "";
        }
        return dataType + ' ' + dataName + ";\n";
    }
    public void parentConstructorCalled(){
	    parentConstructorCalled = true;
    }
    public boolean isParentConstructorCalled(){
	    return parentConstructorCalled;
    }
    public String getDataName(){
	    return dataName;
    }
    public void setPrimaryParent(InstanceInfo parent){
        instanceType.setPrimaryParent(parent);
        for(var method:parent.getMethodsOfType(FunctionType.VIRTUAL)){
           for(var v:method.getVariants()){
               appendToInitializer(getDataName() + "." + v.implName() +"=" + v.cname() + ";\n");
           }
        }
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
            instanceType.setDestructorName(prefix);
            b.append(prefix).append("_destructor(").append(instanceType.getCname());
            b.append("* this){\n").append(destructor).append("}\n");
            return b.toString();
        }
	    return "";
    }
    public String getImplicitDestructorCode(){
	    return destructor.toString();
    }
    public AEnv getSuperEnv(){
	    return superEnv;
    }
    public void compileNewAndAlloc(BuilderWriter writer,TypeInfo[]args,InstanceVar constructor){
        var allocName = "static" + getNameSpace() + "_alloc";
        var allocMethod = (Function)classType.getFieldFromThisType("alloc");
        allocMethod.addStaticVariant(args,allocName);
        writer.write(instanceType.getCname() + "* " + allocMethod.getVariant(args).cname() + "(");
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
        var cname = instanceType.getCname();
        writer.write(compiledArgs + "){\n");
        writer.write(cname + "* p=malloc(sizeof(");
        writer.write(cname + "));\n");
        writer.write(constructorCall(constructor,"p",args));
        writer.write("return p;\n}\n");
        var newName = "static" + nameSpace + "_new";
        var newMethod = (Function)classType.getFieldFromThisType("new");
        newMethod.addStaticVariant(args,newName);
        writer.write(cname + " " + newMethod.getVariant(args).cname() + "(" + compiledArgs + "){\n");
        writer.write(cname + " inst;\n");
        writer.write(constructorCall(constructor,"&inst",args));
        writer.write("return inst;\n}\n");
    }
    private String constructorCall(Function constructor,String self,TypeInfo[]args){
        var b = new StringBuilder();
        var v = constructor.getVariant(args);
        b.append(v.cname()).append("(").append(self);
        for(int i = 0; i < v.args().length;++i){
            b.append(",a").append(i);
        }
        return  b.append(");\n").toString();
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
}