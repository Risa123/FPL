package risa.fpl.env;

import java.util.HashMap;

import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.SetAccessModifier;
import risa.fpl.function.block.Constructor;
import risa.fpl.function.block.Fn;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.IField;
import risa.fpl.function.statement.Var;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.TypeInfo;

public final class ClassEnv extends ANameSpacedEnv {
	private final HashMap<String, IField>fields = new HashMap<>();
	private final StringBuilder defaultConstructor = new StringBuilder();
	private final String cname,nameSpace;
	private final StringBuilder methodCode = new StringBuilder();
	private final ClassInfo classType;
	private final TypeInfo instanceType;
	public ClassEnv(ModuleEnv superEnv,String cname,String id) {
		super(superEnv);
		super.addFunction("this",new Constructor());
		super.addFunction("protected",new SetAccessModifier(AccessModifier.PROTECTED));
		this.nameSpace = superEnv.getNameSpace(null) + cname;
		this.cname = cname;
		classType = new ClassInfo(id);
		instanceType = new TypeInfo(id,cname,null);
		instanceType.setClassInfo(classType);
	}
	public void addFields(TypeInfo type) {
		fields.forEach((name,field)->{
		    type.addField(name,field);
		    if(field instanceof Function f){
		        type.appendToDeclaration(f.getDeclaration());
            }
        });
	}
	@Override
	public void addFunction(String name,IFunction value) {
		if(value instanceof IField field) {
			fields.put(name,field);
		}
		super.addFunction(name, value);
	}
	public void appendToDefaultConstructor(String code) {
		defaultConstructor.append(code);
	}
	public String getDefaultConstructorCode() {
		return defaultConstructor.toString();
	}
	public String getDefaultConstructor(){
	    var b = new StringBuilder("void ");
	    b.append(nameSpace);
	    b.append("__init(");
	    b.append(cname);
	    b.append("* this){\n");
	    b.append(defaultConstructor);
	    b.append("}\n");
	    return b.toString();
    }
    public void addMethod(String name,Function method,String code){
         fields.put(name,method);
         methodCode.append(code);
    }
    public String getMethodCode(){
	    return methodCode.toString();
    }
    public ClassInfo getClassType(){
	    return classType;
    }
    public TypeInfo getInstanceType(){
	    return instanceType;
    }
    @Override
    public String getNameSpace(IFunction caller){
	    if(caller instanceof Var){
	        return "";
        }
	    if(caller instanceof Fn && !hasModifier(Modifier.NATIVE) && accessModifier == AccessModifier.PRIVATE){
	        return "static ";
        }
	    return nameSpace;
    }
}