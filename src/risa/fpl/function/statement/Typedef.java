package risa.fpl.function.statement;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.CustomTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Typedef implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        writer.write("typedef ");
        var type = it.nextID();
        if(env.hasTypeInCurrentEnv(type.getValue()) && env.getType(type) instanceof CustomTypeInfo t && t.isPrimitive()){
            throw new CompilerException(type,"type " + type + " is already defined");
        }
        if(env.hasFunctionInCurrentEnv(type.getValue())){
            throw new CompilerException(type,"type cannot be declared identifier for declaration function occupied");
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
        if(it.checkTemplate()){
           t =  IFunction.generateTypeFor(t,originalType,it,env,false);
        }
        if(t == null){
            throw new CompilerException(originalType,"type " + originalType + " not found");
        }
        var b = new BuilderWriter(writer);
        b.write(t.getCname());
        b.write(' ');
        b.write(IFunction.toCId(type.getValue()));
        b.write(after);
        writer.write(b.getCode());
        env.addType(type.getValue(),new CustomTypeInfo(type.getValue(),t,b.getCode()));
        return TypeInfo.VOID;
    }
}
