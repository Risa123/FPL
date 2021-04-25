package risa.fpl.tokenizer;

public record Token(int line,int tokenNum,String value,TokenType type){}