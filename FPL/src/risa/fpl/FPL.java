package risa.fpl;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

import risa.fpl.env.ProgramEnv;

public final class FPL {
	private final String cc,output;
	final String outputDirectory;
	private final PrintStream errStream;
	final ProgramEnv env = new ProgramEnv();
	private final HashMap<String,ModuleBlock>modules = new HashMap<>();
    public FPL(String project,String cc,String output,PrintStream errStream) throws IOException, CompilerException {
    	this.cc = cc;
    	this.output = output;
    	this.errStream = errStream;
    	outputDirectory = project + "/output";
    	try {
    		Files.walk(Paths.get(project + "/src")).filter(p->p.toString().endsWith(".fpl")).forEach(p->{
        	
        		try {
        			var mod = new ModuleBlock(this,p);
    				modules.put(mod.name,mod);
    			} catch (IOException e) {
    				throw new UncheckedIOException(e);
    			}catch(CompilerException e) {
    				throw new UncheckedCompilerException(e);
    			}
        	});
    	}catch(UncheckedCompilerException e) {
    		throw e.ex;
    	}
    }
    public void compile() throws IOException, CompilerException {
    	var path = Paths.get(outputDirectory);
        if(Files.exists(path)){
            var list = Files.walk(path).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for(var p:list) {
                Files.deleteIfExists(p);
            }
        }
    	Files.createDirectory(path);
    	var files = new StringBuilder();
    	for(var name:modules.keySet()) {
    		var mod = getModule(name);
    		mod.compile();
    		files.append(' ');
    		files.append(mod.cfile);
    	}
    	var err = Runtime.getRuntime().exec(cc + " -o " + output + files).getErrorStream();
        errStream.print(new String(err.readAllBytes()));
    }
    ModuleBlock getModule(String name) throws IOException, CompilerException {
    	var mod = modules.get(name);
    	if(mod != null) {
    		mod.compile();
    	}
    	return mod;
    }
	public static void main(String[] args) throws IOException {
		if(args.length != 3) {
			System.err.println("<project directory><c compiler><output file> expected");
			System.exit(1);
		}
		try {
			new FPL(args[0],args[1],args[2],System.err).compile();
		} catch (CompilerException e) {
			System.err.println(e.getMessage());
			System.exit(2);
		}
	}
}