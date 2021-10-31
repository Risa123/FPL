package risa.fpl.info;

import risa.fpl.CompilerException;
import risa.fpl.ModuleBlock;
import risa.fpl.env.*;
import risa.fpl.function.IFunction;
import risa.fpl.function.TemplateArgument;
import risa.fpl.function.block.ClassBlock;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class TemplateTypeInfo extends InstanceInfo{
    private List block;
    private ArrayList<InterfaceInfo>interfaces;
    private LinkedHashMap<String,TypeInfo>templateArgs;
    private final ArrayList<InstanceInfo>generatedTypes = new ArrayList<>();
    private ArrayList<TypeInfo> typesForDeclaration;
    public TemplateTypeInfo(String name,ModuleEnv module,String nameSpace){
        super(name,module,nameSpace);
    }
    public InstanceInfo generateTypeFor(ArrayList<Object>args,AEnv env,int line,int charNum)throws CompilerException,IOException{
       var argsInfo = new ArrayList<TypeInfo>(args.size());
       for(var arg:args){
           if(arg instanceof TemplateArgument t){
            arg = t.type().generateTypeFor(t.args(),env,line,charNum);
           }
           argsInfo.add((TypeInfo)arg);
       }
       var nameBuilder = new StringBuilder();
       for(var arg:argsInfo){
           nameBuilder.append(arg);
       }
       InstanceInfo type = null;
       for(var t:generatedTypes){
           if(t.getName().endsWith(nameBuilder.toString())){
               type = t;
               break;
           }
       }
       if(type == null){
           var superMod = getModule();
           var cname = IFunction.createTemplateTypeCname(getCname(),argsInfo.toArray(new TypeInfo[0]));
           var file = superMod.getNameSpace() + cname  + ".c";
           if(!(env instanceof IClassOwnedEnv e && e.getClassType() != null && e.getClassType().getInstanceType() instanceof TemplateTypeInfo)){
               superMod.addInstanceFile(file);
           }
           var path = Paths.get(superMod.getFPL().getOutputDirectory() + "/" + file);
           var writer = Files.newBufferedWriter(path);
           var mod = new ModuleEnv(superMod,new ModuleBlock(path,superMod.getFPL().getSrcDir(),superMod.getFPL()),cname);
           var name = getName() + nameBuilder;
           var cEnv = new ClassEnv(mod,name,TemplateStatus.GENERATING,false);
           if(argsInfo.size() != templateArgs.size()){
               //space to make the number separate form line:tokenNum
               throw new CompilerException(line,charNum," " + templateArgs.size() + " arguments expected instead of " + argsInfo.size());
           }
           for(int i = 0;i < templateArgs.size();++i){
               var typeName = (String)templateArgs.keySet().toArray()[i];
               var t = argsInfo.get(i);
               cEnv.addType(typeName,t);
               if(t instanceof InstanceInfo instance){
                   cEnv.addFunction(typeName,instance.getConstructor());
               }
           }
           mod.importModules();
           new ClassBlock(false).compileClassBlock(writer,cEnv,mod,new Atom(0,0, name,TokenType.ID),block,interfaces,TemplateStatus.GENERATING);
           type = cEnv.getInstanceType();
           Files.delete(path);
           if(!(env instanceof IClassOwnedEnv e && e.getClassType() != null && e.getClassType().getInstanceType() instanceof TemplateTypeInfo)){
               if(env instanceof ANameSpacedEnv e){
                   e.addTemplateInstance(type);
               }else{
                   ((FnSubEnv)env).addTemplateInstance(type);
               }
           }
           mod.addType(name,type);
           writer.write(cEnv.getDataDefinition());
           writer.write(mod.getFunctionCode());
           generatedTypes.add(type);
           if(mod.getInitializerCall() != null){
               getModule().appendToInitializer(mod.getInitializerCall());
           }
           if(!(env instanceof IClassOwnedEnv e && e.getClassType() != null && e.getClassType().getInstanceType() instanceof TemplateTypeInfo)){
               mod.getFPL().addTemplateCompData(new TemplateCompData(mod,path,cEnv.getDataDefinition() + mod.getFunctionCode(),typesForDeclaration));
           }
       }
       return type;
    }
    public void setDataForGeneration(List block,ArrayList<InterfaceInfo>interfaces,LinkedHashMap<String,TypeInfo>templateArgs){
        this.block = block;
        this.interfaces = interfaces;
        this.templateArgs = templateArgs;
    }
    public void setTypesForDeclaration(ArrayList<TypeInfo>types){
        typesForDeclaration = new ArrayList<>(types);
    }
    @Override
    public String getDeclaration(){
        return "";
    }
}