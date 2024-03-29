package risa.fpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import risa.fpl.env.ClassEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.block.AThreePassBlock;
import risa.fpl.function.block.ExpressionInfo;
import risa.fpl.function.exp.Function;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.Parser;
import risa.fpl.parser.AtomType;

public final class ModuleBlock extends AThreePassBlock{
   private final String cPath,name,sourceFile;
   private boolean compiled;
   private final ModuleEnv env;
   private final ArrayList<ClassEnv>classEnvList = new ArrayList<>();
   private CompilerException lastEx;
   private final ArrayList<ExpressionInfo>expInfos;
   private String mainFunctionCode;
   public ModuleBlock(Path sourceFile,Path srcDir)throws IOException,CompilerException{
       this.sourceFile = sourceFile.subpath(srcDir.getNameCount(),sourceFile.getNameCount()).toString();
	   cPath = FPL.getOutputDirectory() + '/' + this.sourceFile.replace(File.separatorChar,'_') + ".c";
       name = this.sourceFile.replace(File.separatorChar,'.').substring(0,this.sourceFile.lastIndexOf('.'));
       env = new ModuleEnv(FPL.getEnv(),this,null);//has to be initialized here to prevent NPE
       try{
           expInfos = createInfoList(new Parser(Files.newBufferedReader(sourceFile)).parse());
	   }catch(CompilerException e){
		   e.setSourceFile(this.sourceFile);
		   throw e;
	   }
   }
   public void compile()throws CompilerException{
       try{
           if(!(name.equals("std.lang") || name.equals("std.backend"))){
               env.addModuleToImport(new Atom(0,0,"std.lang", AtomType.ID));
           }
           compile(new StringBuilder(),env,expInfos);
           if(!env.isMainDeclared() && isMain()){
               throw new CompilerException("declaration of main expected");
           }
           if(!compiled){
               switch(name){
                   case "std.lang"->{
                       makeMethod("toString","boolToString",TypeInfo.BOOL);
                       makeMethod("isDigit",TypeInfo.CHAR);
                       makeMethod("isControl",TypeInfo.CHAR);
                       makeMethod("isWhitespace",TypeInfo.CHAR);
                       makeMethod("isUpperCase",TypeInfo.CHAR);
                       makeMethod("isLowerCase",TypeInfo.CHAR);
                       makeMethod("isBlank",TypeInfo.CHAR);
                       makeMethod("isHexDigit",TypeInfo.CHAR);
                       makeMethod("isPrint",TypeInfo.CHAR);
                       makeMethod("isPunct",TypeInfo.CHAR);
                       makeMethod("toLowerCase",TypeInfo.CHAR);
                       makeMethod("toUpperCase",TypeInfo.CHAR);
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
                       makeMethod("toString","floatingPointToString",NumberInfo.FLOAT,false);
                       makeMethod("toString","floatingPointToString",NumberInfo.DOUBLE);
                       addNumberFields(ClassInfo.BYTE);
                       addNumberFields(ClassInfo.SBYTE);
                       addNumberFields(ClassInfo.UBYTE);
                       addNumberFields(ClassInfo.SHORT);
                       addNumberFields(ClassInfo.SSHORT);
                       addNumberFields(ClassInfo.USHORT);
                       addNumberFields(ClassInfo.INT);
                       addNumberFields(ClassInfo.SINT);
                       addNumberFields(ClassInfo.UINT);
                       addNumberFields(ClassInfo.LONG);
                       addNumberFields(ClassInfo.SLONG);
                       addNumberFields(ClassInfo.ULONG);
                       addNumberFields(ClassInfo.FLOAT);
                       addNumberFields(ClassInfo.DOUBLE);
                   }
                   case "std.backend"->FPL.setFreeArray((Function)env.getFunctionFromModule("free[]"));
                   case "std.system"->env.getAndMakeInaccessible("callOnExitHandlers");
               }
           }
       }catch(CompilerException ex){
           ex.setSourceFile(sourceFile);
           throw ex;
       }
       compiled = true;
   }
   public void writeToFile()throws IOException{
       try(var writer = Files.newBufferedWriter(Path.of(cPath))){
           env.declare(writer);
           for(var env:classEnvList){
              if(!(env.getInstanceInfo() instanceof TemplateTypeInfo)){
                  writer.write(env.getDataDefinition());
              }
           }
           writer.write(env.getVariableDeclarations());
           writer.write(env.getFunctionDeclarations());
           writer.write(env.getFunctionCode());
           for(var env:classEnvList){
               if(!(env.getInstanceInfo() instanceof TemplateTypeInfo)){
                   writer.write(env.getConstructorCode());
               }
           }
           if(isMain()){//main module is written as last
               writer.write("_String* args;\nvoid onExit();\nvoid _std_system_callOnExitHandlers0();\n");
               if(env.notImportedModule("std.system")){
                   //void* used instead of actual function pointer for simplicity
                   writer.write("void _std_system_addOnExitHandler0(void*);\n");
               }
               writer.write("\nint main(int argc,char** argv){\n");
               writer.write(env.getInitializerCode());
               for(var mod:FPL.getModules()){
                   if(!mod.isMain()){
                       writer.write(mod.env.getInitializerCall());
                   }
               }
               writer.write("_Thread mainThread;\n_std_system_addOnExitHandler0(&onExit);\n");
               writer.write("I_std_lang_Thread_init0(&mainThread,static_std_lang_String_new0(\"Main\",4,0));\n");
               writer.write("_std_lang_currentThread = &mainThread;\n");
               writer.write("void* malloc(" + NumberInfo.MEMORY.getCname() + ");\n");
               writer.write("args = malloc(argc * sizeof(_String));\nfor(int i = 0;i < argc;++i){\n");
               writer.write("unsigned int _std_backend_getCStringLen0(char*);\n");
               writer.write("I_std_lang_String_init0(args + i,argv[i],_std_backend_getCStringLen0(argv[i]),0);\n}\n");
               writer.write(mainFunctionCode);
               writer.write("}\nvoid onExit(){\n");
               for(var mod:FPL.getModules()){
                   writer.write(mod.getEnv().getDestructorCall());
               }
               writer.write("_std_lang_Thread_freeEHEntries0(_std_lang_currentThread);\nfree(args);\n}");
           }else{
               writer.write(env.getInitializer());
               writer.write(env.getDestructor());
           }
       }
   }
   public String getName(){
       return name;
   }
   public String getCPath(){
       return cPath;
   }
   public boolean isMain(){
       return FPL.getMainModule().equals(name);
   }
   private void makeMethod(String name,String oldName,TypeInfo ofType,boolean remove){
      var func = (Function)(remove?env.getAndMakeInaccessible(oldName):env.getFunctionFromModule(oldName));
      if(func == null){
          throw new RuntimeException("internal error: function " + oldName + " not found");
       }
       ofType.addField(name,func.makeMethod(ofType,name));
   }
   private void makeMethod(String name,TypeInfo ofType){
       makeMethod(name,name,ofType,true);
   }
   private void makeMethod(@SuppressWarnings("SameParameterValue")String name,String oldName,TypeInfo ofType){
       makeMethod(name,oldName,ofType,true);
   }
   private void addNumberFields(ClassInfo classInfo){
       var prefix = classInfo.getInstanceInfo().getName().toUpperCase() + '_';
       classInfo.addField("MIN_VALUE",env.getAndMakeInaccessible(prefix + "MIN_VALUE"));
       classInfo.addField("MAX_VALUE",env.getAndMakeInaccessible(prefix + "MAX_VALUE"));
       var n = (NumberInfo)classInfo.getInstanceInfo();
       String name;
       if(n.isFloatingPoint()){
           name = "floatingPointParse";
           if(n == NumberInfo.FLOAT){
               classInfo.addField("NAN",env.getAndMakeInaccessible("FLOAT_NAN"));
               classInfo.addField("POSITIVE_INFINITY",env.getAndMakeInaccessible("FLOAT_POSITIVE_INFINITY"));
               classInfo.addField("NEGATIVE_INFINITY",env.getAndMakeInaccessible("FLOAT_NEGATIVE_INFINITY"));
           }else if(n == NumberInfo.DOUBLE){
               classInfo.addField("NAN",env.getAndMakeInaccessible("DOUBLE_NAN"));
               classInfo.addField("POSITIVE_INFINITY",env.getAndMakeInaccessible("DOUBLE_POSITIVE_INFINITY"));
               classInfo.addField("NEGATIVE_INFINITY",env.getAndMakeInaccessible("DOUBLE_NEGATIVE_INFINITY"));
           }
           makeMethod("isNaN",n);
       }else{
           name = "integerParse";
       }
       classInfo.addField("parse",env.getAndMakeInaccessible(name));
   }
   public ModuleEnv getEnv(){
       return env;
   }
   public ArrayList<ClassEnv>getClassEnvList(){
       return classEnvList;
   }
   public void setLastEx(CompilerException ex){
       lastEx = ex;
   }
   public CompilerException getLastEx(){
       return lastEx;
   }
   public void setMainFunctionCode(String code){
       mainFunctionCode = code;
   }
}