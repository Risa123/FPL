package risa.fpl.info;

import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.block.ClassBlock;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public final class TemplateTypeInfo extends InstanceInfo{
    private List block;
    private ArrayList<InterfaceInfo>interfaces;
    private LinkedHashMap<String,TypeInfo>templateArgs;
    private final HashMap<ArrayList<TypeInfo>,InstanceInfo>generatedTypes = new HashMap<>();
    private final ArrayList<String>instanceFiles = new ArrayList<>();
    public TemplateTypeInfo(String name,ModuleEnv module){
        super(name,module);
    }
    public InstanceInfo generateTypeFor(ArrayList<TypeInfo>args,AEnv env,int line,int charNum)throws CompilerException,IOException{
       if(!generatedTypes.containsKey(args)){
           var mod = getModule();
           var cName = new StringBuilder(getCname());
           for(var arg:args){
               cName.append(arg.getCname());
           }
           var file = mod.getNameSpace() + cName + ".c";
           instanceFiles.add(file);
           try(var writer = Files.newBufferedWriter(Paths.get(mod.getFPL().getOutputDirectory() + "/" + file))){
               var name = new StringBuilder(getName());
               for(var arg:args){
                   name.append(arg.getName());
               }
               var cEnv = new ClassEnv(mod,name.toString(),TemplateStatus.GENERATING);
               if(args.size() != templateArgs.size()){
                   throw new CompilerException(line,charNum," " + templateArgs.size() + " arguments expected instead of " + args.size());
               }
               for(int i = 0;i < templateArgs.size();++i){
                   var typeName = (String)templateArgs.keySet().toArray()[i];
                   var type = args.get(i);
                   cEnv.addType(typeName,type);
                   cEnv.addFunction(typeName,new ClassVariable(type,type.getClassInfo(),new TypeInfo[0],""));
               }
               mod.importModules(writer);
               new ClassBlock().compileClassBlock(writer,cEnv,mod,new Atom(0,0,name.toString(),TokenType.ID),block,interfaces,TemplateStatus.GENERATING);
               writer.write(mod.getVariableDeclarations());
               writer.write(cEnv.getFunctionCode());
               if(env instanceof ANameSpacedEnv e){
                   e.addTemplateInstance(cEnv.getInstanceType());
               }else{
                   ((FnSubEnv)env).addTemplateInstance(cEnv.getInstanceType());
               }
               generatedTypes.put(args,cEnv.getInstanceType());
               return cEnv.getInstanceType();
           }
       }
       return generatedTypes.get(args);
    }
    public void setDataForGeneration(List block,ArrayList<InterfaceInfo>interfaces,LinkedHashMap<String,TypeInfo>templateArgs){
        this.block = block;
        this.interfaces = interfaces;
        this.templateArgs = templateArgs;
    }
    public ArrayList<String>getInstanceFiles(){
        return instanceFiles;
    }
}