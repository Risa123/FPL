package risa.fpl.function;

import risa.fpl.CompilerException;
import risa.fpl.FPL;
import risa.fpl.env.SubEnv;
import risa.fpl.function.block.CompileTimeIfBlock;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public final class CompileTimeIf implements IFunction{
    private String os;
    private boolean noElseIfOrElseIfIsTrue = true;
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        var conditionAtom = it.nextID();
        var condition = conditionAtom.getValue();
        var invert = false;
        if(condition.startsWith("!")){
            invert = true;
            condition = condition.substring(1);
        }
        var isTrue = switch(condition){
            case "flag"-> FPL.hasFlag(it.nextID().getValue());
            case "isInstance"->env.getType(it.nextID()) instanceof InstanceInfo;
            case "isPrimitive"->env.getType(it.nextID()).isPrimitive();
            case "arch"->isOnArchitecture(it.nextID());
            case "os"->isOnOS(it.nextID());
            default->throw new CompilerException(conditionAtom,"there is no condition called " + condition);
        };
        if(invert){
            isTrue = !isTrue;
        }
        var exp = it.next();
        if(isTrue && noElseIfOrElseIfIsTrue){
           if(exp instanceof List list){
               new CompileTimeIfBlock(list).compile(builder,env,it,line,tokenNum);
           }else{
               exp.compile(builder,env,it);
           }
        }
        if(it.hasNext()){
            var next = it.next();
            if(next instanceof List list){
                if(!isTrue){
                    new CompileTimeIfBlock(list).compile(builder,env,it,next.getLine(),next.getTokenNum());
                }
            }else if(next instanceof Atom a && a.getValue().equals("compIf")){
                if(!isTrue){
                    noElseIfOrElseIfIsTrue = true;
                }
                compile(builder,env,it,next.getLine(),next.getTokenNum());
                noElseIfOrElseIfIsTrue = true;
            }else{
                error(next,"compIf or block expected");
            }
        }
        return TypeInfo.VOID;
    }
    private boolean isOnArchitecture(Atom architecture)throws CompilerException{
        var str = architecture.getValue();
        if(str.equals("x64")){
            str = "amd64";
        }else if(!(str.equals("x86") || str.equals("ia64"))){
            error(architecture,"there is no architecture called " + str);
        }
        return System.getProperty("os.arch").equals(str);
    }
    private boolean isOnOS(Atom name)throws CompilerException{
        if(os == null){
            var osName = System.getProperty("os.name").toLowerCase();
            if(osName.contains("win")){
                os = "windows";
            }else if(osName.contains("nix") || osName.contains("nux") || osName.contains("aix")){
                os = "linux";
            }else if(osName.contains("mac")){
                os = "osx";
            }else if(osName.contains("sunos")){
                os = "solaris";
            }else{
                throw new CompilerException("this operating system is not supported");
            }
        }
        return name.getValue().equals(os) || (name.getValue().equals("unixBased") && (os.equals("osx") || os.equals("linus") || os.equals("solaris")));
    }
}