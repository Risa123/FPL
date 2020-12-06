package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.FnEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public abstract class AFunctionBlock extends ABlock{
 protected final TypeInfo[]parseArguments(BufferedWriter writer, ExpIterator it, FnEnv env, TypeInfo owner)throws CompilerException,IOException{
        writer.write('(');
        var args = new ArrayList<TypeInfo>();
        var first = owner == null;
        if(!first){
            writer.write(owner.getCname());
            writer.write("* this");
            env.addFunction("this",new Variable(new PointerInfo(owner),"this","this"));
        }
        while(it.hasNext()) {
            var peeked = it.peek();
            if(peeked instanceof List || ((Atom)peeked).getType() == TokenType.END_ARGS) {
                break;
            }
            if(first) {
                first = false;
            }else {
                writer.write(',');
            }
            var argType = env.getType(it.nextID());
            args.add(argType);
            var argName = it.nextID();
            var argNameCID = IFunction.toCId(argName.getValue());
            if(argType instanceof PointerInfo p && p.isFunctionPointer()){
                writer.write(p.getFunctionPointerDeclaration(argNameCID));
            }else{
                writer.write(argType.getCname());
                writer.write(' ');
                writer.write(argNameCID);
            }
            if(env.hasFunctionInCurrentEnv(argName.getValue())) {
                throw new CompilerException(argName,"there is already argument called " + argName);
            }
            env.addFunction(argName.getValue(),new Variable(argType,IFunction.toCId(argName.getValue()),argName.getValue()));
        }
        writer.write(')');
        var array = new TypeInfo[args.size()];
        args.toArray(array);
        return array;
    }
}