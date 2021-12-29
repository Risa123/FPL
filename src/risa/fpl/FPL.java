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
import risa.fpl.parser.Atom;

public final class FPL{
	private final String output,outputDirectory,mainModule,ccArgs;
	private final ProgramEnv env = new ProgramEnv(this);
	private final HashMap<String,ModuleBlock>modules = new HashMap<>();
	private final ArrayList<String>flags = new ArrayList<>();
	private TypeInfo string;
	private final Path srcDir;
	private Function freeArray;
	private final ArrayList<VariantGenData>functionVariantGenerationData = new ArrayList<>();
	private final ArrayList<TemplateCompData>templateCompData = new ArrayList<>();
    private final StringBuilder files = new StringBuilder();
    public FPL(String project)throws CompilerException{
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
    	output = build.getProperty("outputFile");
    	ccArgs = build.getProperty("ccArgs","");
    	outputDirectory = project + "/output";
        mainModule = build.getProperty("mainModule");
        Collections.addAll(flags,build.getProperty("flags","").split(","));
        srcDir = Path.of(project + "/src");
        try{
            for(var p:Files.walk(srcDir).toList()){
                if(p.toString().endsWith(".fpl")){
                    var mod = new ModuleBlock(p,srcDir,this);
                    modules.put(mod.getName(),mod);
                    files.append(' ').append(mod.getCPath());
                }
            }
        }catch(IOException ex){
            throw new CompilerException(ex.getMessage());
        }
    }
    private void throwBuildFileError(String msg)throws CompilerException{
      var ex = new CompilerException(msg);
      ex.setSourceFile("build.properties");
      throw ex;
    }
    public void compile()throws IOException,CompilerException{
    	var path = Path.of(outputDirectory);
        if(Files.exists(path)){
            for(var p:Files.walk(path).sorted(Comparator.reverseOrder()).toList()){
                Files.delete(p);
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
            for(var mod:list){
                var ex = mod.getLastEx();
                if(ex != null){
                    b.append('\n').append(ex.getMessage());
                }
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
                files.append(' ').append(outputDirectory).append('/').append(file);
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
    	    files.append(' ').append(data.path());
    	    try(var w = Files.newBufferedWriter(data.path())){
    	        w.write(data.module().getDeclarationCode());
                w.write(data.module().getFunctionDeclarations());
    	        w.write(data.code());
            }
        }
    	var err = Runtime.getRuntime().exec("gcc\\bin\\gcc " + ccArgs + " -o " + output + files).getErrorStream();
        System.err.print(new String(err.readAllBytes()));
    }
    public ModuleBlock getModule(String name){
        return modules.get(name);
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
	public static void main(String[] args)throws IOException{
		if(args.length != 1){
			System.err.println("<project directory> expected");
			System.exit(2);
		}
		try{
			new FPL(args[0]).compile();
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
    public Collection<ModuleBlock>getModules(){
        return modules.values();
    }
    public void addTemplateCompData(TemplateCompData data){
        templateCompData.add(data);
    }
}