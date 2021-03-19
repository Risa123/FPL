package risa.fpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.block.ATwoPassBlock;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.parser.Parser;
import risa.fpl.tokenizer.TokenType;

public final class ModuleBlock extends ATwoPassBlock{
   private final String cPath,name,sourceFile;
   private boolean compiled;
   private final List exps;
   private ModuleEnv env;
   private final FPL fpl;
   public ModuleBlock(Path sourceFile,FPL fpl)throws IOException,CompilerException{
       this.sourceFile = sourceFile.subpath(2,sourceFile.getNameCount()).toString();
	   cPath = fpl.getOutputDirectory() + "/" + this.sourceFile.replace(File.separatorChar,'_') + ".c";
	   this.fpl = fpl;
	   var name = new StringBuilder();
	   for(int i = 2;i < sourceFile.getNameCount() - 1;++i){
			name.append(sourceFile.getName(i)).append('.');
	   }
	   name.append(sourceFile.getFileName().toString().split("\\.")[0]);
	   this.name = name.toString();
	   try(var parser = new Parser(Files.newBufferedReader(sourceFile))){
		   exps = parser.parse();
	   }catch(CompilerException e){
		   e.setSourceFile(this.sourceFile);
		   throw e;
	   }
   }
   public void compile()throws IOException,CompilerException{
       if(!compiled){
           compiled = true;
           try(var writer = Files.newBufferedWriter(Paths.get(cPath))){
               env = new ModuleEnv(fpl.getEnv(), this);
               if(!(name.equals("std.lang") || name.equals("std.backend"))){
                   env.addModuleToImport(new Atom(0,0,"std.lang",TokenType.ID));
               }
               var b = new BuilderWriter(writer);
               compile(b,env,exps);
               env.importModules(writer);
               writer.write(b.getCode());
               writer.write(env.getVariableDeclarations());
               writer.write(env.getFunctionDeclarations());
               writer.write(env.getFunctionCode());
               if(name.equals("std.lang")){
                   makeMethod("getLength",TypeInfo.STRING);
                   makeMethod("equals",TypeInfo.STRING);
                   makeMethod("toString",TypeInfo.BOOL);
                   makeMethod("isDigit",TypeInfo.CHAR);
                   makeMethod("isControl",TypeInfo.CHAR);
                   makeMethod("isWhitespace",TypeInfo.CHAR);
                   makeMethod("isUpper",TypeInfo.CHAR);
                   makeMethod("isLower",TypeInfo.CHAR);
                   makeMethod("isBlank",TypeInfo.CHAR);
                   makeMethod("isHexDigit",TypeInfo.CHAR);
                   makeMethod("isPrint",TypeInfo.CHAR);
                   makeMethod("isPunct",TypeInfo.CHAR);
                   makeMethod("toLower",TypeInfo.CHAR);
                   makeMethod("toUpper",TypeInfo.CHAR);
                   makeMethod("new",ClassInfo.STRING);
               }
               if(!isMain()){
                   writer.write(env.getInitializer("_init"));
               }
           }catch(CompilerException ex){
               ex.setSourceFile(sourceFile);
               throw ex;
           }
       }
   }
   public ModuleEnv getModule(Atom name)throws CompilerException,IOException{
	   var mod = fpl.getModule(name.getValue());
	   if(mod == null) {
		   throw new CompilerException(name,"module " + name + " not found");
	   }
	   return mod.env;
   }
   public String getName(){
       return name;
   }
   public String getCPath(){
       return cPath;
   }
   public boolean isMain(){
       return fpl.getMainModule().equals(name);
   }
   private void makeMethod(String name,TypeInfo ofType){
       var func = env.getAndRemove(name);
       if(func == null){
           return;
       }
       ofType.addField(name,func.makeMethod(ofType));
   }
   private void makeMethod(String name,ClassInfo ofClass){
       ofClass.addField(name,env.getAndRemove(name));
   }
   public ModuleEnv getEnv(){
       return env;
   }
}