package risa.fpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import risa.fpl.env.ProgramEnv;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.VariantGenData;
import risa.fpl.info.TemplateCompData;
import risa.fpl.info.TypeInfo;

public final class FPL{
    private static String outputDirectory,mainModule;
    private static final ProgramEnv env = new ProgramEnv();
	private static final HashMap<String,ModuleBlock>modules = new HashMap<>();
	private static final ArrayList<String>flags = new ArrayList<>();
	private static TypeInfo string;
	private static Path srcDir;
	private static Function freeArray;
	private static final ArrayList<VariantGenData>functionVariantGenerationData = new ArrayList<>();
	private static final ArrayList<TemplateCompData>templateCompData = new ArrayList<>();
    private static final ArrayList<String>files = new ArrayList<>();
    private static void throwBuildFileError(String msg)throws CompilerException{
      var ex = new CompilerException(msg);
      ex.setSourceFile("build.properties");
      throw ex;
    }
    public static ModuleBlock getModule(String name){
        return modules.get(name);
    }
    public static String getOutputDirectory(){
        return outputDirectory;
    }
    static ProgramEnv getEnv(){
        return env;
    }
    static String getMainModule(){
        return mainModule;
    }
	public static void main(String[] args)throws IOException{
		if(args.length != 1){
			System.err.println("<project directory> expected");
			System.exit(2);
		}
        var project = args[0];
		try{
            var build = new Properties();
            try{
                build.load(Files.newInputStream(Path.of(project + "/build.properties")));
            }catch(IOException ex){
                throw new CompilerException(ex.getMessage());
            }
            var requiredKeys = Arrays.asList("mainModule","outputFile");
            for(var key:requiredKeys){
                if(!build.containsKey(key)){
                    throwBuildFileError("no property " + key);
                }
            }
            var allowedKeys = new ArrayList<>(requiredKeys);
            allowedKeys.addAll(Arrays.asList("ccArgs","flags"));
            for(var key:build.keySet()){
                if(!allowedKeys.contains((String)key)){
                    throwBuildFileError("no property " + key + " allowed");
                }
            }
            var output = build.getProperty("outputFile");
            var ccArgs = build.getProperty("ccArgs","").strip().replaceAll("\\s+"," ").split(",");
            outputDirectory = project + "/output";
            mainModule = build.getProperty("mainModule");
            Collections.addAll(flags,build.getProperty("flags","").strip().replaceAll("\\s+"," ").split(","));
            srcDir = Path.of(project + "/src");
            try{
               try(var stream = Files.walk(srcDir)){
                   for(var p:stream.toList()){
                       if(p.toString().endsWith(".fpl")){
                           var mod = new ModuleBlock(p,srcDir);
                           modules.put(mod.getName(),mod);
                           files.add(mod.getCPath());
                       }
                   }
               }
            }catch(IOException ex){
                throw new CompilerException(ex.getMessage());
            }
            var path = Path.of(outputDirectory);
            if(Files.exists(path)){
              try(var stream = Files.walk(path)){
                  for(var p:stream.sorted(Comparator.reverseOrder()).toList()){
                      Files.delete(p);
                  }
              }
            }
            Files.createDirectory(path);
            var list = new ArrayList<>(modules.values());
            for(var i = 0;i < 4 && !list.isEmpty();++i){//four passes needed
                var it = list.iterator();
                while(it.hasNext()){
                    var mod = it.next();
                    try{
                        mod.compile();
                        it.remove();
                    }catch(CompilerException ex){
                        mod.setLastEx(ex);
                    }
                }
            }
            if(!list.isEmpty()){
                var b = new StringBuilder();
                var first = true;
                for(var mod:list){
                    var ex = mod.getLastEx();
                    if(ex != null){
                        if(!first){
                            b.append('\n');
                        }
                        b.append(ex.getMessage());
                    }
                    first = false;
                }
                throw new CompilerException(b.toString());
            }
            if(!modules.containsKey(mainModule)){
                throw new CompilerException("main module not found");
            }
            for(var mod:modules.values()){
                mod.getEnv().buildDeclarations();
            }
            for(var data:templateCompData){
                data.module().buildDeclarations();
            }
            for(var mod:modules.values()){
                for(var file:mod.getEnv().getInstanceFiles()){
                    files.add(outputDirectory + '/' + file);
                }
                if(!mod.isMain()){
                    mod.writeToFile();
                }
            }
            getModule(mainModule).writeToFile();
            for(var tData:templateCompData){
                try(var w = Files.newBufferedWriter(tData.path())){
                    var mod = tData.module();
                    for(var type:tData.typesForDeclaration()){
                        mod.addTypesForDeclaration(type);
                    }
                    mod.updateTypesForDeclaration();
                    mod.declare(w);
                    w.write(mod.getFunctionDeclarations());
                    w.write(mod.getFunctionCode());
                    w.write(tData.code());
                }
            }
            for(var data:functionVariantGenerationData){
                files.add(data.path().toString());
                try(var w = Files.newBufferedWriter(data.path())){
                    w.write(data.module().getDeclarationCode());
                    w.write(data.module().getFunctionDeclarations());
                    w.write(data.code());
                }
            }
            var i = 3;
            var cmd = new String[i + ccArgs.length + files.size()];
            cmd[0] = "gcc\\bin\\gcc";
            cmd[1] = "-o";
            cmd[2] = output;
            for(var arg:ccArgs){
                cmd[i] = arg;
                i++;
            }
            for(var file:files){
                cmd[i] = file;
                i++;
            }
            System.err.print(new String(Runtime.getRuntime().exec(cmd).getErrorStream().readAllBytes()));
		}catch (CompilerException e){
			System.err.println(e.getMessage());
			System.exit(3);
		}
	}
    public static boolean hasFlag(String name){
        return flags.contains(name);
    }
    public static TypeInfo getString(){
        return string;
    }
    public static void setString(TypeInfo stringType){
        string = stringType;
    }
    public static Path getSrcDir(){
        return srcDir;
    }
    public static Function getFreeArray(){
        return freeArray;
    }
    public static  void setFreeArray(Function f){
        freeArray = f;
    }
    public static void addFunctionVariantGenerationData(VariantGenData data){
        if(!functionVariantGenerationData.contains(data)){
            functionVariantGenerationData.add(data);
        }
    }
    public static Collection<ModuleBlock>getModules(){
        return modules.values();
    }
    public static void addTemplateCompData(TemplateCompData data){
        templateCompData.add(data);
    }
}