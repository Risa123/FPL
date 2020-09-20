package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class GetObjectInfo extends AField{
    private final TypeInfo returnType;
    private final String field;
    private final InstanceInfo self;
    public GetObjectInfo(TypeInfo returnType, String field, InstanceInfo self){
        this.returnType = returnType;
        this.field = field;
        this.self = self;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        writer.write("(" + self.getClassDataType());
        writer.write(')');
        writePrev(writer);
        writer.write(".object_data)->" + field);
        return  returnType;
    }
}