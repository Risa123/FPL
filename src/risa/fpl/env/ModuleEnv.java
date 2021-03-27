package risa.fpl.env;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.ModuleBlock;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.block.ATwoPassBlock;
import risa.fpl.function.block.Main;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

public final class ModuleEnv extends ANameSpacedEnv{
	private final ArrayList<ModuleEnv>importedModules = new ArrayList<>();
	private final ModuleBlock moduleBlock;
	private final String nameSpace;
	private boolean getRequestFromOutSide,initCalled;
	private final StringBuilder variableDeclarations = new StringBuilder();
	private int mainDeclared;
	private final ArrayList<TypeInfo>typesForDeclarations = new ArrayList<>();
	public ModuleEnv(AEnv superEnv,ModuleBlock moduleBlock){
		super(superEnv);
		this.moduleBlock = moduleBlock;
		nameSpace = IFunction.toCId(moduleBlock.getName().replace('.','_'));
		if(moduleBlock.isMain()){
		    addFunction("main",new Main());
        }
	}
	private void addRequiredTypes(TypeInfo type,ArrayList<TypeInfo>types){
	    for(var t:type.getRequiredTypes()){
	       if(t.notIn(types)){
               types.add(t);
               addRequiredTypes(t,types);
           }
        }
    }
	public void importModules(BufferedWriter writer)throws CompilerException,IOException{
	    var types = new ArrayList<TypeInfo>();
	    for(var mod:importedModules){
	        if(mod.importedModules.contains(this)){
	            throw new CompilerException(0,0,"recursive dependency of module " + moduleBlock.getName() + " in module " + mod.moduleBlock.getName());
            }
	        for(var type:mod.types.values()){
	            if(type.notIn(types)){
	                types.add(type);
	                addRequiredTypes(type,types);
                }
            }
        }
	    declareTypes(writer,types);
		for(var mod:importedModules){
            for(var func:mod.functions.values()){
                if(func instanceof Function f && f.getAccessModifier() != AccessModifier.PRIVATE){
                    writer.write(f.getDeclaration());
                }else if(func instanceof Variable v){
                    writer.write(v.getExternDeclaration());
                }
            }
        }
	}
	@Override
	public TypeInfo getType(Atom name)throws CompilerException{
		for(var mod:importedModules){
			if(mod.hasTypeInCurrentEnv(name.getValue())){
			    return mod.getType(name);
            }
		}
		return super.getType(name);
	}
	@Override
	public IFunction getFunction(Atom name)throws CompilerException{
		for(var mod:importedModules){
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
	public String getNameSpace(IFunction caller){
	    if(!hasModifier(Modifier.NATIVE) && accessModifier == AccessModifier.PRIVATE){
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
        typesForDeclarations.add(type);
        for(var t:type.getRequiredTypes()){
           if(!t.isPrimitive() && t.notIn(typesForDeclarations)){
               typesForDeclarations.add(t);
           }
        }
    }
    public void requestFromOutSide(){
	    getRequestFromOutSide = true;
    }
    public boolean isMain(){
	    return moduleBlock.isMain();
    }
    public void initCalled(){
	    initCalled = true;
    }
    @Override
    public ModuleEnv getModule(){
	    return this;
    }
    public Function getAndRemove(String name){
		return (Function)functions.remove(name);
	}
	public String getVariableDeclarations(){
	    return variableDeclarations.toString();
    }
    public void appendVariableDeclaration(String code){
	    variableDeclarations.append(code);
    }
    public void addModuleToImport(Atom module)throws CompilerException,IOException{
	    importedModules.add(moduleBlock.getModule(module));
    }
    public boolean allDependenciesInitCalled(){
	    for(var m:importedModules){
	        if(!m.initCalled){
	            return false;
            }
        }
	    return true;
    }
    public ArrayList<ModuleEnv>getImportedModules(){
	    return importedModules;
    }
    public ArrayList<String>getInstanceFiles(){
	    var files = new ArrayList<String>();
	    for(var type:types.values()){
	        if(type instanceof TemplateTypeInfo t){
	            files.addAll(t.getInstanceFiles());
            }
        }
	    return files;
    }
    public void declareMain(){
	    mainDeclared++;
    }
    public boolean isMainDeclared(){
	    return mainDeclared > 0;
    }
    public boolean multipleMainDeclared(){
	    return mainDeclared == ATwoPassBlock.MAX_PASSES;
    }
    private void declareTypes(BufferedWriter writer,ArrayList<TypeInfo>types)throws IOException{
        var declared = new ArrayList<TypeInfo>();
        while(!types.isEmpty()){
            var it = types.iterator();
            while(it.hasNext()){
                var t = it.next();
                var hasAll = true;
                for(var rt:t.getRequiredTypes()){
                    if(rt.notIn(declared) && !rt.notIn(types)){
                        hasAll = false;
                        break;
                    }
                }
                if(hasAll){
                    declared.add(t);
                    writer.write(t.getDeclaration());
                    it.remove();
                }
            }
        }
    }
    public void declareTypes(BufferedWriter writer)throws IOException{
	    declareTypes(writer,typesForDeclarations);
    }
    @Override
    public void addType(String name,TypeInfo type,boolean declaration){
	    super.addType(name,type,declaration);
	    if(!type.isPrimitive()){
            addTypesForDeclaration(type);
        }
    }
    private void addTypesForDeclaration(TypeInfo type){
	   if(!type.notIn(types.values()) && type.notIn(typesForDeclarations)){
           typesForDeclarations.add(type);
           for(var t:type.getRequiredTypes()){
               addTypesForDeclaration(t);
           }
       }
    }
}