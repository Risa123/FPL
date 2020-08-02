package risa.fpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.block.ATwoPassBlock;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.parser.Parser;

public final class ModuleBlock extends ATwoPassBlock {
   private final FPL lang;
   private final String cfile,name,sourceFile;
   private boolean compiled;
   private final List exps;
   private ModuleEnv env;
   public ModuleBlock(FPL lang,Path sourceFile) throws IOException, CompilerException {
	   this.lang = lang;
       this.sourceFile = sourceFile.subpath(2, sourceFile.getNameCount()).toString();
	   cfile = lang.outputDirectory + "/" + this.sourceFile.replace(File.separatorChar,'_') + ".c";
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
           try (var writer = Files.newBufferedWriter(Paths.get(cfile))) {
               env = new ModuleEnv(lang.env, this);
               compile(writer,env,exps);
           }catch(CompilerException ex) {
               ex.setSourceFile(sourceFile);
               throw ex;
           }
       }
   }
   public ModuleEnv getModule(Atom name) throws CompilerException, IOException {
	   var mod = lang.getModule(name.getValue());
	   if(mod == null) {
		   throw new CompilerException(name,"module " + name + " not found");
	   }
	   return mod.env;
   }
   public String getName(){
       return name;
   }
   public String getCFile(){
       return cfile;
   }
}