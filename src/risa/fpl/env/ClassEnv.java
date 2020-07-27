package risa.fpl.env;

import java.util.HashMap;

import risa.fpl.function.IFunction;
import risa.fpl.function.exp.AField;
import risa.fpl.info.TypeInfo;

public final class ClassEnv extends SubEnv {
	private final HashMap<String,AField>fields = new HashMap<>();
	private final StringBuilder constructor = new StringBuilder();
	public ClassEnv(AEnv superEnv) {
		super(superEnv);
	}
	public void addFields(TypeInfo type) {
		fields.forEach(type::addField);
	}
	@Override
	public void addFunction(String name,IFunction value) {
		if(value instanceof AField field) {
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
}