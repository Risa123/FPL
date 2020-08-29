package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
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
        if(env.hasTypeInCurrentEnv(id.getValue())){
            throw new CompilerException(id,"this type is already declared");
        }
        if(!IFunction.isCId(cID)){
            throw new CompilerException(id,"invalid C identifier");
        }
        var b  = new BuilderWriter(writer);
        b.write("typedef struct " + cID + "{\n");
        var sEnv = new CStructEnv(env,cID);
        it.nextList().compile(b,sEnv,it);
        b.write("}" + cID + ";\n");
        var type = sEnv.getType();
        writer.write(b.getCode());
        type.appendToDeclaration(b.getCode());
        type.buildDeclaration();
        return  TypeInfo.VOID;
    }
}