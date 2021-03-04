package risa.fpl.info;

import risa.fpl.CompilerException;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.ModuleEnv;
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
    private final Atom id = new Atom(0,0,getName(),TokenType.ID);
    private List block;
    private ArrayList<InterfaceInfo>interfaces;
    private LinkedHashMap<String,TypeInfo>templateArgs;
    private final ArrayList<String>templateFiles = new ArrayList<>();
    public TemplateTypeInfo(String name,ModuleEnv module){
        super(name,module);
    }
    public InstanceInfo generateTypeFor(ArrayList<TypeInfo>args)throws CompilerException,IOException{
        var mod = getModule();
        var cEnv = new ClassEnv(mod,getName(),false);
        var cName = new StringBuilder(getCname());
        for(int i = 0;i < templateArgs.size();++i){
            cName.append('_');
            cName.append(args.get(i).getCname());
            cEnv.addType((String)templateArgs.keySet().toArray()[i],args.get(i));
        }
        var path = mod.getFPL().getOutputDirectory() + "/" + mod.getNameSpace().substring(1) + cName +".c";
        templateFiles.add(path);
        var writer = Files.newBufferedWriter(Paths.get(path));
        new ClassBlock().compileClassBlock(writer,cEnv,mod,id,block,interfaces,false);
        writer.write(cEnv.getFunctionDeclarations());
        writer.write(cEnv.getFunctionCode());
        writer.close();
        return cEnv.getInstanceType();
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