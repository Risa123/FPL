package risa.fpl.env;


import risa.fpl.CompilerException;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.AddModifier;
import risa.fpl.function.SetAccessModifier;
import risa.fpl.function.block.Constructor;
import risa.fpl.function.exp.Cast;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.exp.IField;
import risa.fpl.function.statement.Var;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;

public final class ClassEnv extends ANameSpacedEnv implements IClassOwnedEnv{
	private final StringBuilder implicitConstructor = new StringBuilder();
	private final String nameSpace,dataType,dataName;
	private final ClassInfo classType;
	private final InstanceInfo instanceType;
	private final StringBuilder implBuilder = new StringBuilder();
	private boolean parentConstructorCalled;
	private static final SetAccessModifier PROTECTED = new SetAccessModifier(AccessModifier.PROTECTED);
	private static final SetAccessModifier INTERNAL = new SetAccessModifier(AccessModifier.INTERNAL);
	private static final AddModifier VIRTUAL = new AddModifier(Modifier.VIRTUAL);
	private static final AddModifier OVERRIDE = new AddModifier(Modifier.OVERRIDE);
	public ClassEnv(ModuleEnv superEnv,String id,TemplateStatus templateStatus){
		super(superEnv);
		super.addFunction("this",new Constructor());
		super.addFunction("protected",PROTECTED);
		super.addFunction("virtual",VIRTUAL);
		super.addFunction("override",OVERRIDE);
		super.addFunction("internal",INTERNAL);
		var cname = IFunction.toCId(id);
		this.nameSpace = superEnv.getNameSpace(null) + cname;
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
        /*
         *checking if not generating from template to prevent generated type form displacing the template
         */
		if(templateStatus != TemplateStatus.GENERATING){
            superEnv.addType(id,instanceType);
        }
		appendToInitializer(dataName + ".size=sizeof(" + cname +");\n");
	}
	@Override
	public void addFunction(String name,IFunction value){
		if(value instanceof IField field) {
			instanceType.addField(name,field);
        }else{
		    super.addFunction(name,value);
        }
	}
	public void appendToImplicitConstructor(String code) {
		implicitConstructor.append(code);
	}
	public String getImplicitConstructorCode(){
	    var additionalCode = "";
        var primaryParent = instanceType.getPrimaryParent();
        //check for implicit constructor
        if(primaryParent != null && primaryParent.getConstructor().getArguments().length == 0){
            additionalCode += primaryParent.getConstructor().getCname() + "((" + primaryParent.getCname() + "*)this);\n";
        }
	    if(!isAbstract()){
	        additionalCode += "this->object_data=&" + dataName + ";\n";
	     }
		return additionalCode + implicitConstructor.toString();
	}
	public String getImplicitConstructor(){
        return "void " + IFunction.INTERNAL_METHOD_PREFIX +  nameSpace + "_init(" + instanceType.getCname() + "* this){\n" + getImplicitConstructorCode() + "}\n";
    }
    public void addMethod(Function method,String code){
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
         	 if(parent != null && parent.getField(method.getName(),this) instanceof  Function){
         	 	return;
			 }
             implBuilder.append(new PointerInfo(method).getFunctionPointerDeclaration(method.getImplName()));
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
	    b.append(dataType);
	    b.append("{\nunsigned long size;\n");
	    if(primaryParent != null){
	        b.append(primaryParent.getImplCode());
        }
	    b.append(implBuilder).append('}');
	    b.append(dataType).append(";\n");
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
            appendToInitializer(getDataName() + "." + method.getImplName() +"=" + method.getCname() + ";\n");
        }
    }
}