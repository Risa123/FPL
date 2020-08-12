package risa.fpl.env;


import risa.fpl.CompilerException;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.ModifierBlockStat;
import risa.fpl.function.SetAccessModifier;
import risa.fpl.function.block.Constructor;
import risa.fpl.function.exp.Cast;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.exp.IField;
import risa.fpl.function.statement.Var;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

public final class ClassEnv extends ANameSpacedEnv implements IClassOwnedEnv {
	private final StringBuilder defaultConstructor = new StringBuilder();
	private final String cname,nameSpace;
	private final StringBuilder methodCode = new StringBuilder(),methodDeclarations = new StringBuilder();
	private final ClassInfo classType;
	private final TypeInfo instanceType;
	private final StringBuilder implBuilder = new StringBuilder();
	private final String implName;
	private String toParentDeclaration;
	private boolean parentConstructorCalled;
	public ClassEnv(ModuleEnv superEnv,String cname,String id) {
		super(superEnv);
		super.addFunction("this",new Constructor());
		super.addFunction("protected",new SetAccessModifier(AccessModifier.PROTECTED));
		super.addFunction("virtual",new ModifierBlockStat(Modifier.VIRTUAL));
		super.addFunction("override",new ModifierBlockStat(Modifier.OVERRIDE));
		this.nameSpace = superEnv.getNameSpace(null) + cname;
		this.cname = cname;
		classType = new ClassInfo(id);
		instanceType = new TypeInfo(id,cname);
		instanceType.setClassInfo(classType);
		instanceType.addField("cast",new Cast(instanceType));
		implName = cname + "_impl_type";
	}
	@Override
	public void addFunction(String name,IFunction value) {
		if(value instanceof IField field) {
			instanceType.addField(name,field);
        }else{
		    super.addFunction(name, value);
        }
	}
	public void appendToDefaultConstructor(String code) {
		defaultConstructor.append(code);
	}
	public String getDefaultConstructorCode() {
		return defaultConstructor.toString();
	}
	public String getDefaultConstructor(){
	    var b = new StringBuilder("void ");
	    b.append(IFunction.INTERNAL_METHOD_PREFIX);
	    b.append(nameSpace);
	    b.append("_init(");
	    b.append(cname);
	    b.append("* this){\n");
	    var primaryParent = instanceType.getPrimaryParent();
	    if(primaryParent != null){
	        b.append(primaryParent.getConstructor().getCname());
	        b.append("(this);\n");
        }
	    b.append(defaultConstructor);
	    b.append("}\n");
	    return b.toString();
    }
    public void addMethod(Function method,String code){
         if(method.getAccessModifier() == AccessModifier.PRIVATE && !hasModifier(Modifier.NATIVE)){
              methodCode.append("static ");
         }
         if(method.getType() != FunctionType.ABSTRACT){
             methodCode.append(code);
         }
         if(method.getAccessModifier() != AccessModifier.PRIVATE){
             methodDeclarations.append(method.getDeclaration());
         }
         if(method.isVirtual()){
             implBuilder.append(new PointerInfo(method).getFunctionPointerDeclaration(method.getCname()));
             implBuilder.append(";\n");
         }
    }
    public String getMethodCode(){
	    return methodCode.toString();
    }
    @Override
    public ClassInfo getClassType(){
	    return classType;
    }
    public TypeInfo getInstanceType(){
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
    public void appendDeclarations(){
        instanceType.appendToDeclaration(methodDeclarations.toString());
    }
    @Override
    public IFunction getFunction(Atom name) throws CompilerException {
	    var field = instanceType.getField(name.getValue(),this);
	    if(field != null){
	        return field;
        }
	    return super.getFunction(name);
    }
    public boolean isAbstract(){
	    return superEnv.hasModifier(Modifier.ABSTRACT);
    }
    public String getImpl(){
	    var b = new StringBuilder("typedef struct ");
	    b.append(implName);
	    b.append("{\n");
	    b.append(implBuilder);
	    b.append('}');
	    b.append(implName);
	    b.append(";\n");
	    var primaryParent = instanceType.getPrimaryParent();
	    if(primaryParent != null){
	        toParentDeclaration = primaryParent.getCname() + " " + IFunction.INTERNAL_METHOD_PREFIX + getNameSpace() + "_toParent()";
	        b.append(toParentDeclaration);
	        b.append(";\n");
        }
	    return b.toString();
    }
    public String getImplOf(InterfaceInfo i){
	    return cname + i.getCname() + "_impl";
    }
    public String getImplDefinition(){
	    var b = new StringBuilder();
        b.append("static ");
        b.append(implName);
        b.append(' ');
        b.append(cname);
        b.append("_impl;\n");
        var primaryParent = instanceType.getPrimaryParent();
        if(primaryParent != null){
            b.append(toParentDeclaration);
            b.append("{}\n");
        }
        return b.toString();
    }
    public void parentConstructorCalled(){
	    parentConstructorCalled = true;
    }
    public boolean isParentConstructorCalled(){
	    return parentConstructorCalled;
    }
}