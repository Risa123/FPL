package risa.fpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import risa.fpl.env.ClassEnv;
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
   private final ArrayList<ClassEnv>classEnvList = new ArrayList<>();
   public ModuleBlock(Path sourceFile,Path srcDir,FPL fpl)throws IOException,CompilerException{
       var subPath = sourceFile.subpath(srcDir.getNameCount(),sourceFile.getNameCount());
       this.sourceFile = subPath.toString();
	   cPath = fpl.getOutputDirectory() + "/" + this.sourceFile.replace(File.separatorChar,'_') + ".c";
	   this.fpl = fpl;
	   var name = new StringBuilder();
	   for(int i = 0; i < subPath.getNameCount() - 1;i++){
	       name.append(subPath.getName(i)).append('.');
       }
	   name.append(subPath.getFileName().toString().split("\\.")[0]);
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
               env = new ModuleEnv(fpl.getEnv(),this,null);
               if(!(name.equals("std.lang") || name.equals("std.backend"))){
                   env.addModuleToImport(new Atom(0,0,"std.lang",TokenType.ID));
               }
               var b = new BuilderWriter();
               compile(b,env,exps);
               if(!env.isMainDeclared() && isMain()){
                   throw new CompilerException(1,1,"declaration of main expected");
               }
               env.importModules();
               env.declareTypes(writer);
               var tmp = new StringBuilder();
               for(var line:b.getCode().lines().toList()){
                   if(!line.equals(";")){
                       tmp.append(line).append("\n");
                   }
               }
               writer.write(tmp.toString());
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
                   makeMethod("toString","integerToString",NumberInfo.MEMORY,false);
                   makeMethod("toString","integerToString",NumberInfo.BYTE,false);
                   makeMethod("toString","integerToString",NumberInfo.UBYTE,false);
                   makeMethod("toString","integerToString",NumberInfo.SBYTE,false);
                   makeMethod("toString","integerToString",NumberInfo.SHORT,false);
                   makeMethod("toString","integerToString",NumberInfo.USHORT,false);
                   makeMethod("toString","integerToString",NumberInfo.SSHORT);
               }else if(name.equals("std.backend")){
                   fpl.setFreeArray((Function)env.getFunction("free[]"));
               }
               if(!isMain()){
                   writer.write(env.getInitializer());
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
	   if(mod == null){
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
      if(func == null){
          throw new RuntimeException("internal error: function " + name + " not found");
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
   public ArrayList<ClassEnv>getClassEnvList(){
       return classEnvList;
   }
}