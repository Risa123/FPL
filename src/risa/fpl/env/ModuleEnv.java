package risa.fpl.env;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import risa.fpl.CompilerException;
import risa.fpl.FPL;
import risa.fpl.ModuleBlock;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.block.AThreePassBlock;
import risa.fpl.function.block.Main;
import risa.fpl.function.exp.AField;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.function.statement.InstanceVar;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;
import risa.fpl.parser.AtomType;

public final class ModuleEnv extends ANameSpacedEnv{
	private final ArrayList<ModuleEnv>importedModules = new ArrayList<>();
	private final ModuleBlock moduleBlock;
	private String destructorCall,declarationCode = "";//prevent NPE
	private boolean getRequestFromOutSide;
	private final StringBuilder variableDeclarations = new StringBuilder();
	private int mainDeclared;
	private final ArrayList<TypeInfo>typesForDeclarations = new ArrayList<>();
	private final ArrayList<AField>inaccessibleFunctions = new ArrayList<>();
	private final StringBuilder importDeclarations = new StringBuilder();
	private final ArrayList<String>instanceFiles = new ArrayList<>();
    private final ArrayList<InterfaceEnv>interfaceEnvList = new ArrayList<>();
	public ModuleEnv(AEnv superEnv,ModuleBlock moduleBlock,String generatedTemplateCName){
		super(superEnv,generatedTemplateCName == null?IFunction.toCId(moduleBlock.getName().replace('.','_')):((ModuleEnv)superEnv).nameSpace + generatedTemplateCName);
		this.moduleBlock = moduleBlock;
		if(moduleBlock.isMain()){
		    addFunction("main",new Main());
        }
        if(superEnv instanceof ModuleEnv mod){
            for(var m:mod.importedModules){
                if(!importedModules.contains(m)){
                    importedModules.add(m);
                }
            }
        }
        appendFunctionDeclaration("void free(void*);\nvoid _std_backend_freeLEFT_SQUARE_BRACKETRIGHT_SQUARE_BRACKET0(void*,");
        appendFunctionDeclaration(NumberInfo.MEMORY.getCname() + ");\n");
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
        if(name.getType() == AtomType.ID && name.getValue().contains(".")){
            var tmp = name.getValue().split("\\.");
            var modName = String.join(".",Arrays.copyOf(tmp,tmp.length - 1));
            for(var mod:importedModules){
                if(mod.moduleBlock.getName().equals(modName)){
                    return mod.getFunctionFromModule(new Atom(name.getLine(),name.getTokenNum(),tmp[tmp.length - 1],AtomType.ID));
                }
            }
            if(modName.equals(moduleBlock.getName())){
                return getFunctionFromModule(new Atom(name.getLine(),name.getTokenNum(),tmp[tmp.length - 1],AtomType.ID));
            }
            error(name,"module " + modName + " not found");
        }
		return hasFunctionInCurrentEnv(name.getValue())?getFunctionFromModule(name):superEnv.getFunction(name);
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
		return (!hasModifier(Modifier.NATIVE) && getAccessModifier() == AccessModifier.PRIVATE)?"":nameSpace;
	}
    @Override
    public void addTemplateInstance(InstanceInfo type){
        addTypesForDeclaration(type);
    }
    public void requestFromOutSide(){
	    getRequestFromOutSide = true;
    }
    public boolean isMain(){
	    return moduleBlock.isMain();
    }
    @Override
    public ModuleEnv getModule(){
	    return this;
    }
    public AField getAndMakeInaccessible(String name){
	    var f = (AField)functions.get(name);
	    inaccessibleFunctions.add(f);
		return f;
	}
	public String getVariableDeclarations(){
	    return variableDeclarations.toString();
    }
    public void appendVariableDeclaration(String code){
	    variableDeclarations.append(code);
    }
    public void addModuleToImport(Atom module)throws CompilerException{
	    if(module.getValue().equals(moduleBlock.getName())){
	        throw new CompilerException(module,"cannot import current module");
        }
        var block = FPL.getModule(module.getValue());
        if(block == null){
            error(module,"module " +  module + " not found");
        }
        importedModules.add(block.getEnv());
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
    public void declare(BufferedWriter writer)throws IOException{
        var declared = new ArrayList<TypeInfo>();
        var b = new StringBuilder();
        for(var mod:importedModules){
            for(var type:mod.types.values()){
                addTypesForDeclaration(type);
            }
        }
        for(var mod:importedModules){
            for(var func:mod.functions.values()){
                if(func instanceof Function f  && !(func instanceof InstanceVar)  && f.getAccessModifier() != AccessModifier.PRIVATE){
                    importDeclarations.append(f.getDeclaration());
                }else if(func instanceof Variable v){
                    importDeclarations.append(v.getExternDeclaration());
                }
            }
        }
        while(!typesForDeclarations.isEmpty()){
            var it = typesForDeclarations.iterator();
            while(it.hasNext()){
                var t = it.next();
                var hasAll = true;
                for(var rt:t.getRequiredTypes()){
                    if(rt.notIn(declared) && !rt.notIn(typesForDeclarations) && t.notIn(rt.getRequiredTypes())){
                        hasAll = false;
                        break;
                    }
                }
                if(hasAll){
                    declared.add(t);
                    b.append(t.getDeclaration());
                    it.remove();
                }
            }
        }
        for(var type:declared){
            if(type instanceof InstanceInfo i){
                b.append(i.getMethodDeclarations());
            }
        }
        if(superEnv instanceof ModuleEnv mod){
            importDeclarations.append(mod.importDeclarations);
        }
        b.append(importDeclarations);
        writer.write(b.toString());
        declarationCode = b.toString();
    }
    @Override
    public void addType(TypeInfo type,boolean declaration){
	    super.addType(type,declaration);
        addTypesForDeclaration(type);
	    if(nameSpace.equals("_std_lang") && type.getName().equals("String")){
	        FPL.setString(type);
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
            destructorCall = IFunction.INTERNAL_PREFIX + nameSpace + "_destructor()";
            b.append(destructorCall).append("{\n").append(destructor).append("}\n");
            destructorCall += ";\n";
            return b.toString();
        }
        return "";
    }
    public String getDestructorCall(){
        return destructorCall;
    }
    public String getInitializer(){
        return isMain()?"":getInitializer("init");
    }
    public void addInstanceFile(String file){
        if(!instanceFiles.contains(file)){
            instanceFiles.add(file);
        }
    }
    public String getDeclarationCode(){
        return declarationCode;
    }
    /**
     * adds all required types declared after generation of template type
     */
    public void updateTypesForDeclaration(){
        var copy = new ArrayList<>(typesForDeclarations);
        for(var type:copy){
            for(var t:type.getRequiredTypes()){
                addTypesForDeclaration(t);
            }
        }
    }
    public ModuleBlock getModuleBlock(){
        return moduleBlock;
    }
    public IFunction getFunctionFromModule(String name){
        return functions.get(name);
    }
    public void buildDeclarations(){
        for(var type:types.values()){
            if(type instanceof TemplateTypeInfo t){
                t.setTypesForDeclaration(typesForDeclarations);
            }else if(type instanceof NonTrivialTypeInfo t){
                t.buildDeclaration();
            }
        }
    }
    public ArrayList<InterfaceEnv>getInterfaceEnvList(){
        return interfaceEnvList;
    }
}