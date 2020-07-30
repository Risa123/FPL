package risa.fpl.function.exp;

import risa.fpl.env.AEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Size extends AField {
    private final String cname;
    public Size(String cname){
        this.cname = cname;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException{
        writer.write("sizeof ");
        writer.write(cname);
        return NumberInfo.MEMORY;
    }
}