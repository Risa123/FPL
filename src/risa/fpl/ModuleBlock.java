package risa.fpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.block.ATwoPassBlock;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.parser.Parser;
import risa.fpl.tokenizer.TokenType;

public final class ModuleBlock extends ATwoPassBlock {
   private final String cFile,name,sourceFile;
   private boolean compiled;
   private final List exps;
   private ModuleEnv env;
   private final FPL fpl;
   public ModuleBlock(Path sourceFile,FPL fpl) throws IOException, CompilerException {
       this.sourceFile = sourceFile.subpath(2, sourceFile.getNameCount()).toString();
	   cFile = fpl.getOutputDirectory() + "/" + this.sourceFile.replace(File.separatorChar,'_') + ".c";
	   this.fpl = fpl;
		var name = new StringBuilder();
		for(int i = 2;i < sourceFile.getNameCount() - 1;++i) {
			name.append(sourceFile.getName(i));
			name.append('.');
		}
		name.append(sourceFile.getFileName().toString().split("\\.")[0]);
		this.name = name.toString();
	   try (var parser = new Parser(Files.newBufferedReader(sourceFile))){
		   exps = parser.parse();
	   }catch(CompilerException e) {
		   e.setSourceFile(this.sourceFile);
		   throw e;
	   }
   }
   public void compile() throws IOException, CompilerException {
       if (!compiled) {
           compiled = true;
           try (var writer = Files.newBufferedWriter(Paths.get(cFile))) {
               env = new ModuleEnv(fpl.getEnv(), this);
               writer.write("#include<setjmp.h>\n");
               if(!name.equals("std.lang")){
                   env.importModule(new Atom(0,0,"std.lang", TokenType.ID),writer);
               }
               compile(writer,env,exps);
               if(!isMain()){
                   writer.write(env.getInitializer("_init"));
               }
           }catch(CompilerException ex) {
               ex.setSourceFile(sourceFile);
               throw ex;
           }
       }
   }
   public ModuleEnv getModule(Atom name) throws CompilerException, IOException {
	   var mod = fpl.getModule(name.getValue());
	   if(mod == null) {
		   throw new CompilerException(name,"module " + name + " not found");
	   }
	   return mod.env;
   }
   public String getName(){
       return name;
   }
   public String getCFile(){
       return cFile;
   }
   public boolean isMain(){
       return fpl.getMainModule().equals(name);
   }
   public ArrayList<ModuleEnv> getModuleEnvironments(){
       var modBlocks = fpl.getModules();
       var list = new ArrayList<ModuleEnv>(modBlocks.size());
       for(var block:modBlocks){
           list.add(block.env);
       }
       return list;
   }
}