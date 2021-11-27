package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Break implements IFunction{
	@Override
	public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException{
		writer.write("break");
		return TypeInfo.VOID;
	}
}