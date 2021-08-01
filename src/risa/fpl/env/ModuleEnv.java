package risa.fpl.env;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.ModuleBlock;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.block.AThreePassBlock;
import risa.fpl.function.block.Main;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.function.exp.VariantGenData;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.tokenizer.TokenType;

public final class ModuleEnv extends ANameSpacedEnv{
	private final ArrayList<ModuleEnv>importedModules = new ArrayList<>();
	private final ModuleBlock moduleBlock;
	private final String nameSpace;
	private String destructorCall;
	private boolean getRequestFromOutSide,initCalled,destructorCalled;
	private final StringBuilder variableDeclarations = new StringBuilder();
	private int mainDeclared;
	private final ArrayList<TypeInfo>typesForDeclarations = new ArrayList<>();
	private final ArrayList<Function>inaccessibleFunctions = new ArrayList<>();
	private final ArrayList<Integer>classConstructorLines = new ArrayList<>();
	private final StringBuilder importDeclarations = new StringBuilder();
	private final ArrayList<String>instanceFiles = new ArrayList<>();
	private final ArrayList<VariantGenData>functionVariantGenerationData = new ArrayList<>();
	public ModuleEnv(AEnv superEnv,ModuleBlock moduleBlock,String generatedTemplateCName){
		super(superEnv);
		this.moduleBlock = moduleBlock;
		if(generatedTemplateCName == null){//template generation
            nameSpace = IFunction.toCId(moduleBlock.getName().replace('.','_'));
        }else{
            nameSpace = ((ModuleEnv)superEnv).nameSpace + generatedTemplateCName;
        }
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
	public void importModules(){
	    if(superEnv instanceof ModuleEnv mod){
	        for(var m:mod.importedModules){
	            if(!importedModules.contains(m)){
	                importedModules.add(m);
                }
            }
        }
	    for(var mod:importedModules){
	        for(var type:mod.types.values()){
	            if(type.notIn(typesForDeclarations) && !type.isPrimitive()){
	                typesForDeclarations.add(type);
	                addRequiredTypes(type,typesForDeclarations);
                }
            }
        }
		for(var mod:importedModules){
            for(var func:mod.functions.values()){
                if(func instanceof Function f  && !(func instanceof ClassVariable)  && f.getAccessModifier() != AccessModifier.PRIVATE){
                    importDeclarations.append(f.getDeclaration());
                }else if(func instanceof Variable v){
                    importDeclarations.append(v.getExternDeclaration());
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
			   return mod.getFunctionFromModule(name);
            }
		}
        if(name.getType() == TokenType.ID && name.getValue().contains(".")){
            var tmp = name.getValue().split("\\.");
            var modName = String.join(".",Arrays.copyOf(tmp,tmp.length - 1));
            for(var mod:importedModules){
                if(mod.moduleBlock.getName().equals(modName)){
                    return mod.getFunctionFromModule(new Atom(name.getLine(),name.getTokenNum(),tmp[tmp.length - 1], TokenType.ID));
                }
            }
            throw new CompilerException(name,"module " + modName + " not found");
        }
        if(hasFunctionInCurrentEnv(name.getValue())){
            return getFunctionFromModule(name);
        }
		return superEnv.getFunction(name);
	}
	private IFunction getFunctionFromModule(Atom name)throws CompilerException{
        var f = super.getFunction(name);
        if(f instanceof Function func){
            if(func.getAccessModifier() == AccessModifier.PUBLIC && (!inaccessibleFunctions.contains(f) || !getRequestFromOutSide)){
                getRequestFromOutSide = false;
                return f;
            }else if(!getRequestFromOutSide && !inaccessibleFunctions.contains(f)){
                return f;
            }
        }else{
            getRequestFromOutSide = false;
            return f;
        }
        getRequestFromOutSide = false;
	    throw new CompilerException(name,"function " +  name + " not found");
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
      if(!type.isPrimitive() && type.notIn(typesForDeclarations)){
         addTypesForDeclaration(type);
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
    public Function getAndMakeInaccessible(String name){
	    var f = (Function)functions.get(name);
	    inaccessibleFunctions.add(f);
		return f;
	}
	public String getVariableDeclarations(){
	    return variableDeclarations.toString();
    }
    public void appendVariableDeclaration(String code){
	    variableDeclarations.append(code);
    }
    public void addModuleToImport(Atom module)throws CompilerException,IOException{
	    if(module.getValue().equals(moduleBlock.getName())){
	        throw new CompilerException(module,"recursive dependency");
        }
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
	    return instanceFiles;
    }
    public void declareMain(){
	    mainDeclared++;
    }
    public boolean isMainDeclared(){
	    return mainDeclared > 0;
    }
    public boolean multipleMainDeclared(){
	    return mainDeclared == AThreePassBlock.MAX_PASSES;
    }
    public void declareTypes(BufferedWriter writer)throws IOException{
        var declared = new ArrayList<TypeInfo>();
        var b = new BuilderWriter(writer);
        while(!typesForDeclarations.isEmpty()){
            var it = typesForDeclarations.iterator();
            while(it.hasNext()){
                var t = it.next();
                var hasAll = true;
                for(var rt:t.getRequiredTypes()){
                    if(rt.notIn(declared) && !rt.notIn(typesForDeclarations)){
                        hasAll = false;
                        break;
                    }
                }
                if(hasAll){
                    declared.add(t);
                    b.write(t.getDeclaration());
                    it.remove();
                }
            }
        }
        for(var type:declared){
            if(type instanceof InstanceInfo i){
                b.write(i.getMethodDeclarations());
            }
        }
        if(superEnv instanceof ModuleEnv mod){
            importDeclarations.append(mod.importDeclarations);
        }
        b.write(importDeclarations.toString());
        writer.write(b.getCode());
        for(var data:functionVariantGenerationData){
            try(var w = Files.newBufferedWriter(data.path())){
                w.write(b.getCode());
                w.write(data.code());
            }
        }
    }
    @Override
    public void addType(String name,TypeInfo type,boolean declaration){
	    super.addType(name,type,declaration);
	    if(!type.isPrimitive()){
            addTypesForDeclaration(type);
        }
	    if(nameSpace.equals("_std_lang") && name.equals("String")){
	        moduleBlock.setString(type);
        }
    }
    public void addTypesForDeclaration(TypeInfo type){
	   if(!type.isPrimitive() && type.notIn(typesForDeclarations)){
           typesForDeclarations.add(type);
           for(var t:type.getRequiredTypes()){
               addTypesForDeclaration(t);
           }
       }
    }
    public String getDestructor(){
        if(destructor.isEmpty()){
            destructorCall = "";
        }else{
            var b = new StringBuilder("void ");
            destructorCall = IFunction.INTERNAL_METHOD_PREFIX + getNameSpace() + "_destructor";
            b.append(destructorCall);
            destructorCall += "();\n";
            b.append("(){\n").append(destructor).append("}\n");
            return b.toString();
        }
        return "";
    }
    public final String getDestructorCall(){
        return destructorCall;
    }
    public void destructorCalled(){
	    destructorCalled = true;
    }
    public boolean allDependenciesDestructorCalled(){
        for(var m:importedModules){
            if(!m.destructorCalled){
                return false;
            }
        }
        return true;
    }
    /**
     * Add line where constructor is declared on list.
     * Prevents constructors form being compiled multiple times
     * @param line
     */
    public void addClassConstructorLine(int line){
	    classConstructorLines.add(line);
    }
    public boolean notClassConstructorOnLine(int line){
        return !classConstructorLines.contains(line);
    }
    public String getInitializer(){
        if(isMain()){
            return "";
        }
        return getInitializer("init");
    }
    public void addInstanceFile(String file){
        instanceFiles.add(file);
    }
    public void addVariantGenerationData(VariantGenData data){
        functionVariantGenerationData.add(data);
    }
}