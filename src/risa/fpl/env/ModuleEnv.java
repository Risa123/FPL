package risa.fpl.env;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.ModuleBlock;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.block.Main;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

public final class ModuleEnv extends ANameSpacedEnv {
	private final ArrayList<ModuleEnv>importedModules = new ArrayList<>();
	private final ModuleBlock moduleBlock;
	private final String nameSpace;
	private boolean getRequestFromOutSide,initCalled;
	private final ArrayList<TypeInfo> cDeclaredTypes = new ArrayList<>();
	public ModuleEnv(AEnv superEnv,ModuleBlock moduleBlock) {
		super(superEnv);
		this.moduleBlock = moduleBlock;
		nameSpace = IFunction.toCId(moduleBlock.getName().replace('.','_'));
		if(moduleBlock.isMain()){
		    addFunction("main",new Main());
        }
	}
	public void  importModule(Atom name,BufferedWriter writer) throws CompilerException, IOException {
		var mod = moduleBlock.getModule(name);
		importedModules.add(mod);
        var typesToImport = new ArrayList<>(mod.types.values());
        while(!typesToImport.isEmpty()){
           var it = typesToImport.iterator();
           while(it.hasNext()) {
               var type = it.next();
               var importedRequiredTypes = new ArrayList<>(type.getRequiredTypes());
               var rIt = importedRequiredTypes.iterator();
               while(rIt.hasNext()){
                   var t = rIt.next();
                   if(TypeInfo.notContains(typesToImport,t) && TypeInfo.notContains(cDeclaredTypes,t)){
                       writer.write(t.getDeclaration());
                       cDeclaredTypes.add(t);
                       rIt.remove();
                   }
               }
               if (declaredContains(importedRequiredTypes)) {
                   writer.write(type.getDeclaration());
                   it.remove();
                   cDeclaredTypes.add(type);
               }
           }
        }
		for(var func:mod.functions.values()) {
			if(func instanceof Function f) {
				if(f.getAccessModifier() != AccessModifier.PRIVATE){
                    writer.write(f.getDeclaration());
                }
			}else if(func instanceof Variable v){
			    writer.write(v.getExternDeclaration());
            }
		}
		if(!mod.initCalled){
		    mod.initCalled = true;
		    writer.write("void ");
		    writer.write(mod.getInitializerCall());
		    appendToInitializer(mod.getInitializerCall());
        }
	}
	@Override
	public TypeInfo getType(Atom name) throws CompilerException {
		for(var mod:importedModules) {
			if(mod.hasTypeInCurrentEnv(name.getValue())){
			    return mod.getType(name);
            }
		}
		return super.getType(name);
	}
	@Override
	public IFunction getFunction(Atom name) throws CompilerException {
		for(var mod:importedModules) {
			if(mod.hasFunctionInCurrentEnv(name.getValue())){
			   mod.requestFromOutSide();
			   return mod.getFunction(name);
            }
		}
		if(hasFunctionInCurrentEnv(name.getValue())){
		    var f = super.getFunction(name);
		    if(f instanceof Function func){
		        if(func.getAccessModifier() == AccessModifier.PUBLIC){
		            getRequestFromOutSide = false;
		            return f;
                }else if(!getRequestFromOutSide){
		            return f;
                }
            }else{
		        getRequestFromOutSide = false;
		        return f;
            }
        }
		getRequestFromOutSide = false;
		return superEnv.getFunction(name);
	}
	@Override
	public String getNameSpace(IFunction caller) {
	    if(!hasModifier(Modifier.NATIVE) && accessModifier == AccessModifier.PRIVATE){
	        return "";
        }
		return nameSpace;
	}
	@Override
    public String getNameSpace(){
	    return nameSpace;
    }
	public void requestFromOutSide(){
	    getRequestFromOutSide = true;
    }
    public boolean isMain(){
	    return moduleBlock.isMain();
    }
    public ArrayList<ModuleEnv> getModuleEnvironments(){
	    return moduleBlock.getModuleEnvironments();
    }
    public void initCalled(){
	    initCalled = true;
    }
    public boolean isInitCalled(){
	    return initCalled;
    }
    private boolean declaredContains(ArrayList<TypeInfo>types){
        for(var type:types){
            if(TypeInfo.notContains(cDeclaredTypes,type)){
                return false;
            }
        }
        return true;
    }
    @Override
    public ModuleEnv getModule(){
	    return this;
    }
}