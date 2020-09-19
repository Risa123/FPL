package risa.fpl.function.exp;

import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Dereference extends AField{
    private final TypeInfo type;
    public Dereference(TypeInfo type){
        this.type = type;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException{
        writer.write("(*");
        writePrev(writer);
        writer.write(')');
        return type;
    }
}