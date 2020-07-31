package risa.fpl.env;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.ModuleBlock;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

public final class ModuleEnv extends SubEnv{
	private final ArrayList<ModuleEnv>importedModules = new ArrayList<>();
	private final ModuleBlock moduleBlock;
	private final String nameSpace;
	public ModuleEnv(AEnv superEnv,ModuleBlock moduleBlock) {
		super(superEnv);
		this.moduleBlock = moduleBlock;
		nameSpace = IFunction.toCId(moduleBlock.name.replace('.','_'));
	}
	public void  importModule(Atom name,BufferedWriter writer) throws CompilerException, IOException {
		var mod = moduleBlock.getModule(name);
		importedModules.add(mod);
		for(var type:mod.types.values()) {
		  writer.write(type.declaration);
		}
		for(var func:mod.functions.values()) {
			if(func instanceof Function f) {
				writer.write(f.declaration);
			}
		}
	}
	@Override
	public TypeInfo getType(Atom name) throws CompilerException {
		for(var mod:importedModules) {
			if(mod.hasTypeInCurrentEnv(name.value)){
			    return mod.getType(name);
            }
		}
		return super.getType(name);
	}
	@Override
	public IFunction getFunction(Atom name) throws CompilerException {
		for(var mod:importedModules) {
			if(mod.hasFunctionInCurrentEnv(name.value)){
			    return mod.getFunction(name);
            }
		}
		return super.getFunction(name);
	}
	public String getNameSpace() {
		return nameSpace;
	}
}