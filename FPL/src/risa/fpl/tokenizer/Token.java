package risa.fpl.tokenizer;

public final class Token {
 public final int line,charNum;	
 public final String value;
 public final TokenType type;
 public Token(int line,int charNum,String value,TokenType type) {
	 this.line = line;
	 this.charNum = charNum;
	 this.value = value;
	 this.type = type;
 }
}