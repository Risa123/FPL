package risa.fpl.env;


import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.AddModifier;
import risa.fpl.function.SetAccessModifier;
import risa.fpl.function.block.Constructor;
import risa.fpl.function.block.Destructor;
import risa.fpl.function.exp.*;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.function.statement.Var;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;


public final class ClassEnv extends ANameSpacedEnv implements IClassOwnedEnv{
	private final StringBuilder implicitConstructor = new StringBuilder();
	private final String nameSpace,dataType,dataName;
	private final ClassInfo classType;
	private final InstanceInfo instanceType;
	private final StringBuilder implBuilder = new StringBuilder();
	private boolean parentConstructorCalled,destructorDeclared;
	private static final SetAccessModifier PROTECTED = new SetAccessModifier(AccessModifier.PROTECTED);
	private static final SetAccessModifier INTERNAL = new SetAccessModifier(AccessModifier.INTERNAL);
	private static final AddModifier VIRTUAL = new AddModifier(Modifier.VIRTUAL);
	private static final AddModifier OVERRIDE = new AddModifier(Modifier.OVERRIDE);
	private static final Destructor DESTRUCTOR = new Destructor();
	private static final Constructor CONSTRUCTOR = new Constructor();
	public ClassEnv(ModuleEnv superEnv,String id,TemplateStatus templateStatus){
		super(superEnv);
		super.addFunction("this",CONSTRUCTOR);
		super.addFunction("protected",PROTECTED);
		super.addFunction("virtual",VIRTUAL);
		super.addFunction("override",OVERRIDE);
		super.addFunction("internal",INTERNAL);
		super.addFunction("-this",DESTRUCTOR);
		var cname = IFunction.toCId(id);
		nameSpace = superEnv.getNameSpace() + cname;
		classType = new ClassInfo(id);
		if(templateStatus == TemplateStatus.TEMPLATE){
            instanceType = new TemplateTypeInfo(id,superEnv);
        }else{
            instanceType = new InstanceInfo(id,superEnv);
        }
		instanceType.setClassInfo(classType);
		instanceType.addField("cast",new Cast(instanceType));
		dataType = cname + "_data_type";
		dataName = cname + "_data";
        //checking if not generating from template to prevent generated type from displacing the template
		if(templateStatus != TemplateStatus.GENERATING){
            superEnv.addType(id,instanceType);
        }
		appendToInitializer(dataName + ".size=sizeof(" + cname +");\n");
	}
	@Override
	public void addFunction(String name,IFunction value){
		if(value instanceof IField field){
			instanceType.addField(name,field);
			if(value instanceof Variable v && v.getType() instanceof InstanceInfo i){
			    appendToDestructor(i.getDestructorName());
			    appendToDestructor("(&this->");
			    appendToDestructor(v.getCname());
			    appendToDestructor(");\n");
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
	    if(!isAbstract()){
	        additionalCode += "this->object_data=&" + dataName + ";\n";
	     }
		return additionalCode + implicitConstructor;
	}
	public String getImplicitConstructor(){
        return "void " + IFunction.INTERNAL_METHOD_PREFIX +  nameSpace + "_init0(" + instanceType.getCname() + "* this){\n" + getImplicitConstructorCode() + "}\n";
    }
    public void addMethod(Function method,TypeInfo[] args,String code){
         if(method.getAccessModifier() == AccessModifier.PRIVATE && !hasModifier(Modifier.NATIVE)){
              appendFunctionCode("static ");
         }
         if(method.getType() != FunctionType.ABSTRACT){
             appendFunctionCode(code);
         }
         if(method.getAccessModifier() != AccessModifier.PRIVATE){
             appendFunctionDeclaration(method);
         }
         if(method.isVirtual()){
         	 var parent = instanceType.getPrimaryParent();
         	 if(parent != null && parent.getField(method.getName(),this) instanceof Function){
         	 	return;
			 }
         	 var v = method.getVariant(args);
             implBuilder.append(new PointerInfo(new FunctionInfo(method)).getFunctionPointerDeclaration(v.implName()));
             implBuilder.append(";\n");
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
        instanceType.appendToDeclaration(getFunctionDeclarations());
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
	    if(!destructor.isEmpty()){
            var b = new StringBuilder("void ");
            var prefix = IFunction.INTERNAL_METHOD_PREFIX + nameSpace;
            instanceType.setDestructorName(prefix);
            b.append(prefix);
            b.append("_destructor(").append(instanceType.getCname());
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
    public void compileNewAndAlloc(BuilderWriter writer, TypeInfo[]args, ClassVariable constructor){
        var allocName = "static" + getNameSpace() + "_alloc";
        Function allocMethod;
        if(classType.getFieldFromThisType("alloc") instanceof Function tmp){
            allocMethod = tmp;
        }else{
            allocMethod = new Function("alloc",new PointerInfo(instanceType),AccessModifier.PUBLIC);
            classType.addField("alloc",allocMethod);
        }
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
        writer.write(compiledArgs);
        writer.write("){\n");
        writer.write(instanceType.getCname());
        writer.write("* p=malloc(sizeof(");
        writer.write(instanceType.getCname());
        writer.write("));\n");
        writer.write(constructorCall(constructor,"p",args));
        writer.write("return p;\n}\n");
        var newName = "static" + nameSpace + "_new";
        Function newMethod;
        if(classType.getFieldFromThisType("new") instanceof Function tmp){
            newMethod = tmp;
        }else{
            newMethod = new Function("new",instanceType,AccessModifier.PUBLIC);
            classType.addField("new",newMethod);
        }
        newMethod.addStaticVariant(args,newName);
        writer.write(instanceType.getCname() + " " + newMethod.getVariant(args).cname() + "(" + compiledArgs + "){\n");
        writer.write(instanceType.getCname() + " inst;\n");
        writer.write(constructorCall(constructor,"&inst",args));
        writer.write("return inst;\n}\n");
        appendFunctionDeclaration(newMethod);
        appendFunctionDeclaration(allocMethod);
    }
    private String constructorCall(Function constructor, String self, TypeInfo[]args){
        var b = new StringBuilder();
        var v = constructor.getVariant(args);
        b.append(v.cname()).append("(").append(self);
        for(int i = 0; i < v.args().length;++i){
            b.append(",a").append(i);
        }
        return  b.append(");\n").toString();
    }
}