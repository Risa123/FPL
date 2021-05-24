package risa.fpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.block.AThreePassBlock;
import risa.fpl.function.exp.Function;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;
import risa.fpl.parser.Parser;
import risa.fpl.tokenizer.TokenType;

public final class ModuleBlock extends AThreePassBlock{
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
               env = new ModuleEnv(fpl.getEnv(),this);
               if(!(name.equals("std.lang") || name.equals("std.backend"))){
                   env.addModuleToImport(new Atom(0,0,"std.lang",TokenType.ID));
               }
               var b = new BuilderWriter(writer);
               compile(b,env,exps);
               if(!env.isMainDeclared() && isMain()){
                   throw new CompilerException(1,1,"declaration of main expected");
               }
               env.importModules(writer);
               env.declareTypes(writer);
               writer.write(b.getCode());
               writer.write(env.getVariableDeclarations());
               writer.write(env.getFunctionDeclarations());
               writer.write(env.getFunctionCode());
               if(name.equals("std.lang")){
                   makeMethod("toString","boolToString",TypeInfo.BOOL);
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
                   makeMethod("toString","charToString",TypeInfo.CHAR);
                   makeMethod("toString","integerToString",NumberInfo.INT,false);
                   makeMethod("toString","integerToString",NumberInfo.SINT,false);
                   makeMethod("toString","integerToString",NumberInfo.UINT,false);
                   makeMethod("toString","integerToString",NumberInfo.LONG,false);
                   makeMethod("toString","integerToString",NumberInfo.SLONG,false);
                   makeMethod("toString","integerToString",NumberInfo.ULONG,false);
                   makeMethod("toString","integerToString",NumberInfo.MEMORY);
               }
               if(!isMain()){
                   writer.write(env.getInitializer("_init"));
                   writer.write(env.getDestructor());
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
   private void makeMethod(String name,String oldName,TypeInfo ofType,boolean remove)throws CompilerException{
      Function func;
      if(remove){
          func = env.getAndMakeInaccessible(oldName);
      }else{
          func = (Function)env.getFunction(oldName);
      }
      if (func == null){
          return;
       }
       ofType.addField(name,func.makeMethod(ofType,name));
   }
   private void makeMethod(String name,TypeInfo ofType)throws CompilerException{
       makeMethod(name,name,ofType,true);
   }
   private void makeMethod(String name,String oldName,TypeInfo ofType)throws CompilerException{
       makeMethod(name,oldName,ofType,true);
   }
   public ModuleEnv getEnv(){
       return env;
   }
   public void setString(TypeInfo string){
       fpl.setString(string);
   }
}