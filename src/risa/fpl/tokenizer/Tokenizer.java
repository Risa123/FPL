package risa.fpl.tokenizer;

import java.io.IOException;
import java.io.Reader;

import risa.fpl.CompilerException;

public final class Tokenizer {
	  private final Reader reader;
	  private int line = 1,charNum = 1,c;
	  private boolean readNext = true;
	  private static final int  UBYTE_MAX = 255,USHORT_MAX = 65_535;
	  private static final long UINT_MAX = 4_294_967_295L,ULONG_MAX = Long.parseUnsignedLong("18446744073709551615");
	  private Token current;
	  public Tokenizer(Reader reader) {
		  this.reader = reader;
	  }
	  public void close() throws IOException {
		  reader.close();
	  }
	  public boolean hasNext() throws IOException {
		  return reader.ready() || !readNext;
	  }
	  private Token nextPrivate() throws IOException, CompilerException {
		  read();
		  if(c == '(') {
			  while(hasNext() && read() != ')');
		  }else if(c == '#') {
			  while(hasNext() && read() != '\n');
              readNext = false;
		  }else if(c == '$'){
			  if(!hasNext()) {
				  throw new CompilerException(line,charNum,"char expected");
			  }
			  var builder = new StringBuilder("'");
			  builder.appendCodePoint(read());
			  builder.append("'");
			  return new Token(line,charNum,builder.toString(),TokenType.CHAR);
		  }else if(c == '+' || c == '-' || Character.isDigit(c)) {
			  var signed = false;
			  var b = new StringBuilder();
			  if(c == '+' || c == '-') {
				  b.appendCodePoint(c);
				  read();
				  if(!Character.isDigit(c)) {
					  if(!isSeparator(c)) {
						 b.appendCodePoint(c);
					  }else{
					      readNext = false;
                      }
					  return new Token(line,charNum,b.toString(),TokenType.ID);
				  }
				  signed = true;
			  }
			  b.appendCodePoint(c);
			  var type = signed?TokenType.SINT:TokenType.UINT;
			  var floatingPoint = false;
			  var hasTypeChar = false;
			  while(hasNext() && !isSeparator(read())) {
				  if(Character.isDigit(c)) {
					  b.appendCodePoint(c);
				  }else if(c == '.') {
					 if(floatingPoint) {
						 throw new CompilerException(line,charNum,"this number already has floating point");
					 }
	                 floatingPoint = true;
	                 type = TokenType.DOUBLE;
	                 if(signed){
	                	 throw new CompilerException(line,charNum,"floating point number cannot be signed");
	                 }
				  }else if(c == 'F'){
					  type = TokenType.FLOAT;
					  if(!floatingPoint) {
						  throw new CompilerException(line,charNum,"float number expected");
					  }
					  break;
				  }else if(c == 'L'){
					  hasTypeChar = true;
					  type = signed?TokenType.SLONG:TokenType.ULONG;
					  break;
				  }else if(c == 'S'){
					  hasTypeChar = true;
					  type = signed?TokenType.SSHORT:TokenType.USHORT;
					  break;
				  }else if(c == 'B'){
					  hasTypeChar = true;
					  type = signed?TokenType.SBYTE:TokenType.UBYTE;
					  break;
				  }else {
					  throw new CompilerException(line,charNum,"unexpected character " + c);
				  }
			  }
			  if(!hasTypeChar) {
				  readNext = false;
			  }
			  var value = b.toString();
			  if(floatingPoint) {
				  if(type == TokenType.FLOAT) {
					 try {
						 Float.parseFloat(value);
					 }catch(NumberFormatException e) {
						 throw new CompilerException(line,charNum,"float number expected");
					 }
				  }else if(type == TokenType.DOUBLE) {
					  try {
						  Double.parseDouble(value);
					  }catch(NumberFormatException e) {
						  throw new CompilerException(line,charNum,"double number expected");
					  }
				  }
			  }else {
				  if(type != TokenType.ULONG) {
					  var n = Long.parseLong(value);
					  if(type == TokenType.SBYTE && (n < Byte.MIN_VALUE || n > Byte.MAX_VALUE)) {
						  throw new CompilerException(line,charNum,"sbyte numbere expected");
					  }else if(type == TokenType.SSHORT && (n < Short.MIN_VALUE || n > Short.MAX_VALUE)) {
						  throw new CompilerException(line,charNum,"sshort number expected");
					  }else if(type == TokenType.SINT && (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE)) {
						  throw new CompilerException(line,charNum,"sint number expected");
					  }else if(type == TokenType.SLONG && (n < Long.MIN_VALUE || n > Long.MAX_VALUE)) {
						  throw new CompilerException(line,charNum,"slong number expected");
					  }else if(type == TokenType.UBYTE &&  n > UBYTE_MAX) {
						  throw new CompilerException(line,charNum,"ubyte number expected");
					  }else if(type == TokenType.USHORT && n > USHORT_MAX) {
						  throw new CompilerException(line,charNum,"ushort number expected");
					  }else if(type == TokenType.UINT && n > UINT_MAX) {
						  throw new CompilerException(line,charNum,"uint number expected");
					  }else if(type == TokenType.ULONG && n > ULONG_MAX) {
						  throw new CompilerException(line,charNum,"ulong number expected");
					  }
				  }
			  }
			  return new Token(line,charNum,value,type);
		  } else if( c == '{') {
			  return new Token(line,charNum,"{",TokenType.BEGIN_BLOCK);
		  }else if(c == '}') {
			  return new Token(line,charNum,"}",TokenType.END_BLOCK);
		  }else if(c == '\n') {
			 return new Token(line,charNum,"",TokenType.NEW_LINE) ;
		  }else if(c == ',') {
			  return new Token(line,charNum,",",TokenType.ARG_SEPARATOR);
		  }else if(c == ';') {
			  return new Token(line,charNum,";",TokenType.END_ARGS);
		  } else  if(c == '"'){
			  var b = new StringBuilder();
			  b.append('"');
			  do {
				  read();
				  b.appendCodePoint(c);
			  }while(c != '"');
			  return new Token(line,charNum,b.toString(),TokenType.STRING);
		  }else if(c == ':'){
		      return new Token(line,charNum,":",TokenType.CLASS_SELECTOR);
          } else  if(!isSeparator(c)) {
			  var b = new StringBuilder();
			  readNext = false;
			  while(hasNext() && !isSeparator(read())) {
				  b.appendCodePoint(c);
			  }
			  var str = b.toString();
			  return new Token(line,charNum,str,TokenType.ID);
		  }
		  return null;
	  }
	  public Token next() throws IOException, CompilerException {
	      if(current != null){
	          var r = current;
	          current = null;
	          return r;
          }
		  Token token;
		  while((token = nextPrivate())  == null || token.type() != TokenType.NEW_LINE && (token.value().isEmpty() || token.value().isBlank()));
		  return token;
	  }
	  private int read() throws IOException {
		  if(readNext) {
			 c = reader.read();
			 if(c == '\n') {
				 line++;
				 charNum = 1;
			 }else {
				 charNum++;
			 }
		  }else {
			  readNext = true;
		  }
		  return c;
	  }
	  private boolean isSeparator(int c) {
          if( c == '#' || c == '{' || c == ',' || c == ';' || c == '(' || c == '\n' || c == ':' || c == '}') {
              readNext = false;
              return true;
          }
		  return Character.isWhitespace(c);
	  }
	  public Token peek() throws IOException,CompilerException{
	      if(current == null){
	          current = next();
          }
	      return current;
      }
}