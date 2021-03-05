package risa.fpl.info;

import risa.fpl.CompilerException;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.env.TemplateStatus;
import risa.fpl.function.block.ClassBlock;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
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
    private final ArrayList<String>templateFiles = new ArrayList<>();
    private final HashMap<ArrayList<TypeInfo>,InstanceInfo>generatedTypes = new HashMap<>();
    public TemplateTypeInfo(String name,ModuleEnv module){
        super(name,module);
    }
    public InstanceInfo generateTypeFor(ArrayList<TypeInfo>args,BufferedWriter targetWriter)throws CompilerException,IOException{
       if(!generatedTypes.containsKey(args)){
           var mod = getModule();
           var name = new StringBuilder(getName());
           for(var arg:args){
               name.append(arg.getName());
           }
           var cEnv = new ClassEnv(mod,name.toString(),TemplateStatus.GENERATING);
           var cName = new StringBuilder(getCname());
           for(int i = 0;i < templateArgs.size();++i){
               cName.append('_');
               cName.append(args.get(i).getCname().replaceAll("\\*","_p"));
               cEnv.addType((String)templateArgs.keySet().toArray()[i],args.get(i));
           }
           var path = mod.getFPL().getOutputDirectory() + "/" + mod.getNameSpace().substring(1) + cName +".c";
           templateFiles.add(path);
           var writer = Files.newBufferedWriter(Paths.get(path));
           new ClassBlock().compileClassBlock(writer,cEnv,mod,new Atom(0,0,name.toString(),TokenType.ID),block,interfaces,TemplateStatus.GENERATING);
           writer.write(cEnv.getFunctionDeclarations());
           writer.write(cEnv.getFunctionCode());
           writer.close();
           targetWriter.write(cEnv.getInstanceType().getDeclaration());
           generatedTypes.put(args,cEnv.getInstanceType());
           return cEnv.getInstanceType();
       }
       return generatedTypes.get(args);
    }
    public void setDataForGeneration(List block,ArrayList<InterfaceInfo>interfaces,LinkedHashMap<String,TypeInfo>templateArgs){
        this.block = block;
        this.interfaces = interfaces;
        this.templateArgs = templateArgs;
    }
    public ArrayList<String>getTemplateFiles(){
        return templateFiles;
    }
}