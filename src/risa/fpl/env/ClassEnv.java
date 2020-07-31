package risa.fpl.env;

import java.util.HashMap;

import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.IField;
import risa.fpl.info.TypeInfo;

public final class ClassEnv extends SubEnv {
	private final HashMap<String, IField>fields = new HashMap<>();
	private final StringBuilder constructor = new StringBuilder();
	private final String cname,nameSpace;
	public ClassEnv(ModuleEnv superEnv,String cname) {
		super(superEnv);
		this.nameSpace = superEnv.getNameSpace();
		this.cname = cname;
	}
	public void addFields(TypeInfo type) {
		fields.forEach(type::addField);
	}
	@Override
	public void addFunction(String name,IFunction value) {
		if(value instanceof IField field) {
			fields.put(name,field);
		}
		super.addFunction(name, value);
	}
	public void appendToDefaultConstructor(String code) {
		constructor.append(code);
	}
	public String getDefaultConstructorCode() {
		return constructor.toString();
	}
	public String getDefaultConstructor(){
	    var b = new StringBuilder(nameSpace);
	    b.append(cname);
	    b.append("__init(");
	    b.append(cname);
	    b.append("* this){\n");
	    b.append(constructor);
	    b.append("}\n");
	    return b.toString();
    }
    public void addMethod(String name,Function method,String code){

    }
}