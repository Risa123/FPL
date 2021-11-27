package risa.fpl.parser;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.TypeInfo;

public final class Atom extends AExp{
	private final String value;
	private final AtomType type;
	public Atom(int line,int tokenNum,String value,AtomType type){
		super(line,tokenNum);
		this.value = value;
		this.type = type;
	}
	@Override
	public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it) throws IOException,CompilerException{
		return env.getFunction(this).compile(writer,env,it,getLine(),getTokenNum());
	}
	@Override
	public String toString(){
		return value;
	}
	public String getValue(){
	    return value;
    }
    public AtomType getType(){
	    return type;
    }
    public boolean notIndexLiteral(){
	    return type != AtomType.UINT && type != AtomType.ULONG;
    }
}