package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.FnEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.IPointerInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashMap;

public abstract class AFunctionBlock extends ABlock{
 protected final LinkedHashMap<String,TypeInfo>parseArguments(BufferedWriter writer,ExpIterator it,FnEnv env,TypeInfo owner)throws CompilerException,IOException{
        writer.write('(');
        var args = new LinkedHashMap<String,TypeInfo>();
        var first = owner == null;
        if(!first){
            writer.write(owner.getCname());
            writer.write("* this");
            env.addFunction("this",new Variable(new PointerInfo(owner),"this","this"));
        }
        while(it.hasNext()){
            var peeked = it.peek();
            if(peeked instanceof List || ((Atom)peeked).getValue().equals("=") || ((Atom)peeked).getType() == AtomType.CLASS_SELECTOR){
                break;
            }
            if(first){
                first = false;
            }else{
                writer.write(',');
            }
            var argTypeAtom = it.nextID();
            var argType = env.getType(argTypeAtom);
            var argName = it.nextAtom();
            if(argName.getType() == AtomType.END_ARGS){
                argType = IFunction.generateTypeFor(argType,argTypeAtom,it,env,false);
                argName = it.nextID();
                if(argName.getValue().equals("*")){
                    argType = new PointerInfo(argType);
                    argName = it.nextID();
                }
            }else if(argName.getType() != AtomType.ID){
                throw new CompilerException(argName,"identifier or ; expected");
            }
            if(args.containsKey(argName.getValue())){
                throw new CompilerException(argName,"there is already argument called " + argName);
            }
            args.put(argName.getValue(),argType);
            var argNameCID = IFunction.toCId(argName.getValue());
            if(argType instanceof IPointerInfo p){
                writer.write(p.getPointerVariableDeclaration(argNameCID));
            }else{
                writer.write(argType.getCname());
                writer.write(' ');
                writer.write(argNameCID);
            }
            env.addFunction(argName.getValue(),new Variable(argType,IFunction.toCId(argName.getValue()),argName.getValue()));
        }
        writer.write(')');
        return args;
    }
}