package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Typedef implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        writer.write("typedef ");
        var type = it.nextID();
        if(env.hasTypeInCurrentEnv(type.getValue())){
            throw new CompilerException(type,"type " + type + " is already defined");
        }
        var originalType = it.nextID();
        var after = "";
        if(originalType.getValue().equals("[]")){
           var len = it.nextAtom();
           if(len.notIndexLiteral()){
               throw new CompilerException(len,"array length expected instead of " + len);
           }
           after = "[" + len + "]";
           originalType = it.nextID();
        }
        var t = env.getType(originalType);
        if(t == null){
            throw new CompilerException(originalType,"type " + originalType + " not found");
        }
        writer.write(t.getCname());
        writer.write(' ');
        writer.write(IFunction.toCId(type.getValue()));
        writer.write(after);
        writer.write(";\n");
        env.addType(type.getValue(),t);
        return TypeInfo.VOID;
    }
}
