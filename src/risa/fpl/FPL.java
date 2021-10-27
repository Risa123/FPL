package risa.fpl;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import risa.fpl.env.ProgramEnv;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.VariantGenData;
import risa.fpl.info.TemplateCompData;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;

public final class FPL{
	private final String gcc,output,outputDirectory,mainModule,ccArgs;
	private final PrintStream errStream;
	private final ProgramEnv env = new ProgramEnv(this);
	private final HashMap<String,ModuleBlock>modules = new HashMap<>();
	private final ArrayList<String>flags = new ArrayList<>();
	private TypeInfo string;
	private final Path srcDir;
	private Function freeArray;
	private final ArrayList<VariantGenData>functionVariantGenerationData = new ArrayList<>();
	private final ArrayList<TemplateCompData>templateCompData = new ArrayList<>();
    public FPL(String project,PrintStream errStream)throws IOException,CompilerException{
        var build = new Properties();
        build.load(Files.newInputStream(Paths.get(project + "/build.properties")));
        if(buildFileInvalid(build,Arrays.asList("gcc","mainModule","outputFile"),Arrays.asList("ccArgs","flags"))){
            var ex = new CompilerException(0,0,"invalid build file");
            ex.setSourceFile("build.properties");
            throw ex;
        }
    	gcc = build.getProperty("gcc");
    	output = build.getProperty("outputFile");
    	this.errStream = errStream;
    	ccArgs = build.getProperty("ccArgs","");
    	outputDirectory = project + "/output";
        mainModule = build.getProperty("mainModule");
        Collections.addAll(flags,build.getProperty("flags","").split(","));
        srcDir = Paths.get(project + "/src");
        for(var p:Files.walk(srcDir).collect(Collectors.toList())){
            if(p.toString().endsWith(".fpl")){
                var mod = new ModuleBlock(p,srcDir,this);
                modules.put(mod.getName(),mod);
            }
        }
    }
    private boolean buildFileInvalid(Properties buildFile,List<String>requiredKeys,List<String>optionalKeys){
        var allowedKeys = new ArrayList<>(requiredKeys);
        allowedKeys.addAll(optionalKeys);
        for(var key:requiredKeys){
            if(!buildFile.containsKey(key)){
                return true;
            }
        }
        for(var key:allowedKeys){
            if(!allowedKeys.contains(key)){
                return true;
            }
        }
        return false;
    }
    public void compile()throws IOException,CompilerException{
    	var path = Paths.get(outputDirectory);
        if(Files.exists(path)){
            for(var p:Files.walk(path).sorted(Comparator.reverseOrder()).collect(Collectors.toList())){
                Files.delete(p);
            }
        }
    	Files.createDirectory(path);
    	var files = new StringBuilder();
    	for(var name:modules.keySet()){
    	  if(!name.equals(mainModule)){
              compileModule(name,files);
          }
    	}
    	if(getModule(mainModule) == null){
    	    throw new CompilerException(0,0,"main module not found");
        }
    	compileModule(mainModule,files);
    	for(var mod:modules.values()){
    	    for(var file:mod.getEnv().getInstanceFiles()){
    	        files.append(' ').append(outputDirectory).append('/').append(file);
            }
        }
    	for(var tData:templateCompData){
    	    try(var w = Files.newBufferedWriter(tData.path())){
                var mod = tData.module();
                for(var type:tData.typesForDeclaration()){
                    mod.addTypesForDeclaration(type);
                }
                mod.updateTypesForDeclaration();
    	        mod.declareTypes(w);
                w.write(tData.code());
            }
        }
    	for(var data:functionVariantGenerationData){
    	    files.append(' ').append(data.path());
    	    try(var w = Files.newBufferedWriter(data.path())){
    	        w.write(data.module().getDeclarationCode());
    	        w.write(data.code());
            }
        }
    	var err = Runtime.getRuntime().exec(gcc + "\\bin\\gcc -w " + ccArgs + " -o " + output + files).getErrorStream();
        errStream.print(new String(err.readAllBytes()));
    }
    public ModuleBlock getModule(String name)throws IOException,CompilerException{
    	var mod = modules.get(name);
    	if(mod != null){
    		mod.compile();
    	}
    	return mod;
    }
    public String getOutputDirectory(){
        return outputDirectory;
    }
    ProgramEnv getEnv(){
        return env;
    }
    String getMainModule(){
        return mainModule;
    }
    private void compileModule(String name,StringBuilder files)throws IOException,CompilerException{
        files.append(' ').append(getModule(name).getCPath());
    }
	public static void main(String[] args)throws IOException{
		if(args.length != 1){
			System.err.println("<project directory> expected");
			System.exit(2);
		}
		try{
			new FPL(args[0],System.err).compile();
		}catch (CompilerException e){
			System.err.println(e.getMessage());
			System.exit(3);
		}
	}
    public boolean hasFlag(String name){
        return flags.contains(name);
    }
    public TypeInfo getString(){
        return string;
    }
    void setString(TypeInfo string){
        this.string = string;
    }
    public Path getSrcDir(){
        return srcDir;
    }
    public boolean isOnArchitecture(Atom architecture)throws CompilerException{
        var str = architecture.getValue();
        if(str.equals("x64")){
            str = "amd64";
        }else if(!(str.equals("x86") || str.equals("ia64"))){
            throw new CompilerException(architecture,"there is no architecture called " + str);
        }
        return System.getProperty("os.arch").equals(str);
    }
    public Function getFreeArray(){
        return freeArray;
    }
    public void setFreeArray(Function f){
        freeArray = f;
    }
    public void addFunctionVariantGenerationData(VariantGenData data){
        if(!functionVariantGenerationData.contains(data)){
            functionVariantGenerationData.add(data);
        }
    }
    public void addTemplateCompData(TemplateCompData data){
        templateCompData.add(data);
    }
}