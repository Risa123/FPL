package risa.fpl.parser;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.tokenizer.TokenType;

public final class Atom extends AExp{
	private final String value;
	private final TokenType type;
	public Atom(int line,int charNum,String value,TokenType type){
		super(line, charNum);
		this.value = value;
		this.type = type;
	}
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it) throws IOException,CompilerException{
		return env.getFunction(this).compile(writer,env,it,getLine(),getCharNum());
	}
	@Override
	public String toString(){
		return value;
	}
	public String getValue(){
	    return value;
    }
    public TokenType getType(){
	    return type;
    }
    public boolean notIndexLiteral(){
	    return type != TokenType.UINT && type != TokenType.ULONG;
    }
}