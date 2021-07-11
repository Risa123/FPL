package risa.fpl.function;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.block.CompileTimeIfBlock;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

import java.io.BufferedWriter;
import java.io.IOException;

public final class CompileTimeIf implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env, ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var conditionAtom = it.nextID();
        var condition = conditionAtom.getValue();
        var invert = false;
        if(condition.startsWith("!")){
            invert = true;
            condition = condition.substring(1);
        }
        var isTrue = switch(condition){
            case "flag"-> env.getFPL().hasFlag(it.nextID().getValue());
            case "isInstance"->env.getType(it.nextID()) instanceof InstanceInfo;
            case "isPrimitive"->env.getType(it.nextID()).isPrimitive();
            default -> throw new CompilerException(conditionAtom, "there is no condition called " + condition);
        };
        if(invert){
            isTrue = !isTrue;
        }
        var exp = it.next();
        if(isTrue){
           if(exp instanceof List list){
               new CompileTimeIfBlock(list).compile(writer,env,it,line,tokenNum);
           }else{
               exp.compile(writer,env,it);
           }
        }
        return TypeInfo.VOID;
    }
}