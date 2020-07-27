package risa.fpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import risa.fpl.env.ModuleEnv;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.parser.Parser;

public final class ModuleBlock {
   private final FPL lang;	
   private final String sourceFile;
   public final String cfile,name;
   private boolean compiled;
   private final List exps;
   private ModuleEnv env;
   public ModuleBlock(FPL lang,Path sourceFile) throws IOException, CompilerException {
	   this.lang = lang;
	   this.sourceFile = sourceFile.toString();
	   cfile = lang.outputDirectory + "/" + this.sourceFile.replace(File.separatorChar,'_') + ".c";
		var name = new StringBuilder();
		for(int i = 2;i < sourceFile.getNameCount() - 1;++i) {
			name.append(sourceFile.getName(i));
			name.append('.');
		}
		name.append(sourceFile.getFileName().toString().split("\\.")[0]);
		this.name = name.toString();
	   var parser = new Parser(Files.newBufferedReader(sourceFile));
	   try {
		   exps = parser.parse();
	   }catch(CompilerException e) {
		   e.setSourceFile(this.sourceFile);
		   throw e;
	   }
   }
   public void compile() throws IOException, CompilerException {
	  if(!compiled) {
		  compiled = true;
		  try(var writer = Files.newBufferedWriter(Paths.get(cfile))){
			 env = new ModuleEnv(lang.env,this);
			 var infos = new ArrayList<ExpInfo>(exps.exps.size());
			 for(var exp:exps.exps) {
				 var info = new ExpInfo();
				 info.exp = exp;
				 info.writer = new BuilderWriter(writer);
				 infos.add(info);
			 }
			 for(;;) {
				 var someNoAttempt = false;
				 var it = infos.iterator();
				 while(it.hasNext()) {
					var info = it.next(); 
					if(!info.attemptedToCompile) {
						someNoAttempt = true;
					}
					try {
						 info.exp.compile(info.writer, env,null);
						 it.remove();
						 writer.write(info.writer.getText());
					}catch(CompilerException e) {
						info.attemptedToCompile = true;
						info.lastEx = e;
						info.writer = new BuilderWriter(writer);
						var exps = ((List)info.exp).exps;
						if(!exps.isEmpty() && exps.get(0) instanceof Atom a && a.value.equals("use")) {
							throw e;
						}
					}
				 }
				 if(!someNoAttempt) {
					 if(!infos.isEmpty()) {
						 var b = new StringBuilder("errors in module:");
						 for(var info:infos) {
							 b.append('\n');
							 info.lastEx.setSourceFile(sourceFile);
							 b.append(info.lastEx.getMessage());
						 }
						 var first = infos.get(0).exp;
						 throw new CompilerException(first.line,first.charNum,b.toString());
					 }
					 break;
				 }
			 }
		  }catch(CompilerException e) {
			  e.setSourceFile(sourceFile);
			  throw e;
		  }
	  }
   }
   public ModuleEnv getModule(Atom name) throws CompilerException, IOException {
	   var mod = lang.getModule(name.value);
	   if(mod == null) {
		   throw new CompilerException(name,"module " + name + " not found");
	   }
	   return mod.env;
   }
   private class ExpInfo{
	   AExp exp;
	   CompilerException lastEx;
	   boolean attemptedToCompile;
	   BuilderWriter writer;
   }
}