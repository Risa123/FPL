package risa.fpl.tokenizer;

import java.io.IOException;
import java.io.Reader;

import risa.fpl.CompilerException;

public final class Tokenizer {
	  private final Reader reader;
	  private int line = 1,charNum = 1,c;
	  private boolean readNext = true,forceEnd;
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
	      if(forceEnd){
	          return false;
          }
		  return reader.ready() || !readNext;
	  }
	  private Token nextPrivate() throws IOException, CompilerException {
		  read();
		  if(c == '(') {
			  while(hasNext() && read() != ')');
		  }else if(c == '#') {
			  while(hasNext() && read() != '\n');
			  readNext = c != '\n';
		  }else if(c == '$'){
			  if(!hasNext()) {
				  throw new CompilerException(line,charNum,"char expected");
			  }
			  var builder = new StringBuilder("'");
			  builder.appendCodePoint(read());
			  if(c == '\\' && hasNext()){
				  builder.appendCodePoint(read());
			  }
			  builder.append("'");
			  return new Token(line,charNum,builder.toString(),TokenType.CHAR);
		  }else if(c == '+' || c == '-' || Character.isDigit(c)) {
			  var signed = false;
			  var hex = false;
			  var b = new StringBuilder();
			  if(c == '+' || c == '-') {
				  b.appendCodePoint(c);
				  read();
				  if(!Character.isDigit(c)) {
					  if(notSeparator(c)) {
						 b.appendCodePoint(c);
					  }else{
					      readNext = false;
                      }
					  return new Token(line,charNum,b.toString(),TokenType.ID);
				  }
				  signed = true;
			  }else if(c == '0'){
			  	read();
			  	if(c == 'x'){
                    hex = true;
				}else if(notSeparator(c)){
					readNext = false;
				}
			    if(!hex){
					b.append('0');
				}
			  }else{
				  b.appendCodePoint(c);
			  }
			  var type = signed?TokenType.SINT:TokenType.UINT;
			  var floatingPoint = false;
			  var hasTypeChar = false;
			  if(hex){
                while(hasNext() && notSeparator(read())){
                    b.appendCodePoint(c);
				}
			  }else if(notSeparator(c)){
				  while(hasNext() && notSeparator(read())) {
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
						  throw new CompilerException(line,charNum,"unexpected character " + Character.toString(c) + ",code:" + c);
					  }
				  }
			  }
			  if(!hasTypeChar) {
				  readNext = false;
			  }
			  var value = b.toString();
			  if(value.isEmpty()){
			  	 return null;
			  }
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
					  long n;
					  if(hex){
					  	n = Long.parseLong(value,16);
					  }else{
					  	 n = Long.parseLong(value);
					  }
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
			  if(hex){
			  	 value = "0x" + value;
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
          } else  if(notSeparator(c)) {
			  var b = new StringBuilder();
			  readNext = false;
			  if(!Character.isValidCodePoint(c)){
			      forceEnd = true;
			      return new Token(line,charNum,"",TokenType.NEW_LINE);
              }
			  while(hasNext() && notSeparator(read())) {
				  b.appendCodePoint(c);
			  }
			  return new Token(line,charNum,b.toString(),TokenType.ID);
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
	  private boolean notSeparator(int c) {
          if( c == '#' || c == '{' || c == ',' || c == ';' || c == '(' || c == '\n' || c == ':' || c == '}') {
              readNext = false;
              return false;
          }
		  return !Character.isWhitespace(c);
	  }
	  public Token peek() throws IOException,CompilerException{
	      if(current == null){
	          current = next();
          }
	      return current;
      }
}