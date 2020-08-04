package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.InterfaceEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class InterfaceBlock implements IFunction {
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        if(!(env instanceof ModuleEnv)){
            throw new CompilerException(line,charNum,"interface can only be declared on module level");
        }
        var id = it.nextID();
        var idV = id.getValue();
        if(env.hasTypeInCurrentEnv(idV)){
            throw new CompilerException(id,"this type is already declared");
        }
        var cID = IFunction.toCId(idV);
        var type = new InterfaceInfo(idV,cID);
        List block = null;
        while(it.hasNext()){
            var exp = it.next();
            if(exp instanceof List l){
                block = l;
                break;
            }else{
               var typeID = (Atom)exp;
               if(typeID.getType() != TokenType.ID){
                   throw new CompilerException(id,"identifier expected");
               }
               var parentType = env.getType(typeID);
               if(!(parentType instanceof InterfaceInfo)){
                   throw new CompilerException(typeID,"interface can only inherit from interfaces");
               }
            }
        }
        if(block == null){
            throw new CompilerException(line,charNum,"block expected as last argument");
        }
        writer.write("typedef struct ");
        writer.write(cID);
        writer.write("{\n");
        block.compile(writer,new InterfaceEnv(env),it);
        writer.write('}');
        writer.write(cID);
        writer.write(";\n");
        env.addType(idV,type);
        return TypeInfo.VOID;
    }
}
