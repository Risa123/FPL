package risa.fpl.info;

import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.block.ClassBlock;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public final class TemplateTypeInfo extends InstanceInfo{
    private List block;
    private ArrayList<InterfaceInfo>interfaces;
    private LinkedHashMap<String,TypeInfo>templateArgs;
    private final HashMap<ArrayList<TypeInfo>,InstanceInfo>generatedTypes = new HashMap<>();
    public TemplateTypeInfo(String name,ModuleEnv module){
        super(name,module);
    }
    public InstanceInfo generateTypeFor(ArrayList<TypeInfo>args,AEnv env,int line,int charNum)throws CompilerException,IOException{
       if(!generatedTypes.containsKey(args)){
           var mod = getModule();
           var path = env.getFPL().getOutputDirectory() + "/" + mod.getNameSpace().substring(1) + ".fpl.c";
           var writer = new BufferedWriter(new FileWriter(path,true));
           var name = new StringBuilder(getName());
           for(var arg:args){
               name.append(arg.getName());
               if(!(mod.hasTypeInCurrentEnv(arg.getName()) && mod.getType(new Atom(0,0,arg.getName(),TokenType.ID)).identical(arg))){
                   writer.write(arg.getDeclaration());
               }
           }
           var cEnv = new ClassEnv(mod,name.toString(),TemplateStatus.GENERATING);
           if(args.size() != templateArgs.size()){
               throw new CompilerException(line,charNum," " + templateArgs.size() + " arguments expected instead of " + args.size());
           }
           for(int i = 0;i < templateArgs.size();++i){
               cEnv.addType((String)templateArgs.keySet().toArray()[i],args.get(i));
           }
           new ClassBlock().compileClassBlock(writer,cEnv,mod,new Atom(0,0,name.toString(),TokenType.ID),block,interfaces,TemplateStatus.GENERATING);
           writer.write(cEnv.getFunctionDeclarations());
           writer.write(cEnv.getFunctionCode());
           writer.close();
           if(env instanceof ANameSpacedEnv e){
               e.addTemplateInstance(cEnv.getInstanceType());
           }else{
               ((FnSubEnv)env).addTemplateInstance(cEnv.getInstanceType());
           }
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
}