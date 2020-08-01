package risa.fpl.tokenizer;

public record Token (int line,int charNum,String value,TokenType type){}