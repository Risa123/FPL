package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.CStructEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class CStructBlock extends ABlock {
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        var id = it.nextID();
        var cID = id.getValue();
        if(!IFunction.isCId(cID)){
            throw new CompilerException(id,"invalid C identifier");
        }
        writer.write("typedef struct " + cID + "{\n");
        it.nextList().compile(writer,new CStructEnv(env,cID),it);
        writer.write("}" + cID + ";\n");
        return  TypeInfo.VOID;
    }
}