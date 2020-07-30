package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

//do not remove
public final class New implements IFunction {
    private final TypeInfo type;
    public New(TypeInfo type){
        this.type = type;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {

        return new PointerInfo(type);
    }
}