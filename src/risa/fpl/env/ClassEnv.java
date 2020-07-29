package risa.fpl.env;

import java.util.HashMap;

import risa.fpl.function.IFunction;
import risa.fpl.function.exp.IField;
import risa.fpl.info.TypeInfo;

public final class ClassEnv extends SubEnv {
	private final HashMap<String, IField>fields = new HashMap<>();
	private final StringBuilder constructor = new StringBuilder();
	private final String cname;
	public ClassEnv(AEnv superEnv,String cname) {
		super(superEnv);
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
	    var b = new StringBuilder(cname);
	    b.append("__init(");
	    b.append(cname);
	    b.append("* this){\n");
	    b.append(constructor);
	    b.append("}\n");
	    return b.toString();
    }
}