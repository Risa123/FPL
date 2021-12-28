package risa.fpl.parser;

import java.io.IOException;
import java.io.Reader;

import risa.fpl.CompilerException;

public final class Tokenizer{
	  private final Reader reader;
	  private int line = 1,tokenNum = 1,c;
	  private boolean readNext = true,forceEnd;
	  private static final int  UBYTE_MAX = 255,USHORT_MAX = 65_535;
	  private static final long UINT_MAX = 4_294_967_295L;
	  private Atom current;
	  public Tokenizer(Reader reader){
		  this.reader = reader;
	  }
	  public void close()throws IOException{
		  reader.close();
	  }
	  public boolean hasNext()throws IOException{
	      if(forceEnd){
	          return false;
          }
		  return reader.ready() || !readNext;
	  }
	  private Atom nextPrivate()throws IOException,CompilerException{
		  read();
		  if(c == '('){
			  //noinspection StatementWithEmptyBody
			  while(hasNext() && read() != ')');
		  }else if(c == '#'){
			  //noinspection StatementWithEmptyBody
			  while(hasNext() && read() != '\n');
			  readNext = c != '\n';
		  }else if(c == '$'){
			  if(!hasNext()){
				  throw new CompilerException(line,tokenNum,"char expected");
			  }
			  var builder = new StringBuilder("'");
			  var firstChar = read();
			  if(firstChar > 127){
			  	throw new CompilerException(line,tokenNum,"invalid ascii char " + Character.toString(firstChar));
			  }
			  if(firstChar == '\\' && hasNext()){
			      read();
                  switch(c){
                      case 't','n','f','b','r','\\','0'->{
                          builder.appendCodePoint('\\');
                          builder.appendCodePoint(c);
                      }
                      case 's'->builder.append(' ');
                      default->throw new CompilerException(line,tokenNum,"no special character " + Character.toString(c));
                  }
			  }else if(Character.isWhitespace(firstChar)){
			      throw new CompilerException(line,tokenNum,"$ cannot be followed by whitespace");
              }else{
			      if(c == '\''){
			          builder.append('\\');
                  }
			      builder.appendCodePoint(firstChar);
              }
			  builder.append("'");
			  return new Atom(line,tokenNum,builder.toString(),AtomType.CHAR);
		  }else if(c == '+' || c == '-' || Character.isDigit(c)){
			  var signed = false;
			  var hex = false;
			  var b = new StringBuilder();
			  if(c == '+' || c == '-'){
				  b.appendCodePoint(c);
				  read();
				  if(Character.isDigit(c)){
					 b.appendCodePoint(c);
				  }else{
                      if(notSeparator(c)){
                          b.appendCodePoint(c);
						  if(!Character.isValidCodePoint(c)){
							  forceEnd = true;
							  return new Atom(line,tokenNum,"",AtomType.NEW_LINE);
						  }
						  while(hasNext() && notSeparator(read())){
							  b.appendCodePoint(c);
						  }
                      }else{
                          readNext = false;
                      }
                      return new Atom(line,tokenNum,b.toString(),AtomType.ID);
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
			  var type = signed?AtomType.SINT:AtomType.UINT;
			  var floatingPoint = false;
			  var hasTypeChar = false;
			  var hasScientificNotation = false;
			  if(hex){
                while(hasNext() && notSeparator(read())){
                    b.appendCodePoint(c);
				}
			  }else if(notSeparator(c)){
				  while(hasNext() && notSeparator(read())){
					  if(Character.isDigit(c)){
						  b.appendCodePoint(c);
					  }else if(c == '.'){
						  if(floatingPoint){
							  throw new CompilerException(line,tokenNum,"this number already has floating point");
						  }
						  floatingPoint = true;
						  type = AtomType.DOUBLE;
						  if(signed){
							  throw new CompilerException(line,tokenNum,"floating point number cannot be signed");
						  }
						  b.append('.');
					  }else if(c == 'F'){
						  type = AtomType.FLOAT;
						  if(!floatingPoint){
							  throw new CompilerException(line,tokenNum,"float number expected");
						  }
						  break;
					  }else if(c == 'L'){
						  hasTypeChar = true;
						  type = signed?AtomType.SLONG:AtomType.ULONG;
						  break;
					  }else if(c == 'S'){
						  hasTypeChar = true;
						  type = signed?AtomType.SSHORT:AtomType.USHORT;
						  break;
					  }else if(c == 'B'){
						  hasTypeChar = true;
						  type = signed?AtomType.SBYTE:AtomType.UBYTE;
						  break;
					  }else if(c == 'e'){
                        if(hasScientificNotation){
							throw new CompilerException(line,tokenNum,"this number already has scientific notation");
						}else{
							hasScientificNotation = true;
						}
						b.append('e');
					  }else if(c != '_'){//ignore _ to make literals more readable
						  throw new CompilerException(line,tokenNum,"unexpected character " + Character.toString(c) + ",code:" + c);
					  }
				  }
			  }
			  if(!hasTypeChar){
				  readNext = false;
			  }
			  var value = b.toString();
			  if(value.isEmpty()){
			  	 return null;
			  }
			  if(floatingPoint){
				  if(type == AtomType.FLOAT){
					 try {
						 Float.parseFloat(value);
					 }catch(NumberFormatException e){
						 throw new CompilerException(line,tokenNum,"float number expected");
					 }
				  }else if(type == AtomType.DOUBLE){
					  try{
						  Double.parseDouble(value);
					  }catch(NumberFormatException e){
						  throw new CompilerException(line,tokenNum,"double number expected");
					  }
				  }
			  }else{
				  if(type != AtomType.ULONG){
					  long n;
					  try{
						  if(hex){
							  n = Long.parseLong(value,16);
						  }else{
							  n = Double.valueOf(value).longValue();
						  }
					  }catch(NumberFormatException e){
						  throw new CompilerException(tokenNum,line,"invalid number " + value);
					  }
					  if(type == AtomType.SBYTE && (n < Byte.MIN_VALUE || n > Byte.MAX_VALUE)){
						  throw new CompilerException(line,tokenNum,"sbyte numbere expected");
					  }else if(type == AtomType.SSHORT && (n < Short.MIN_VALUE || n > Short.MAX_VALUE)){
						  throw new CompilerException(line,tokenNum,"sshort number expected");
					  }else if(type == AtomType.SINT && (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE)){
						  throw new CompilerException(line,tokenNum,"sint number expected");
					  }else if(type == AtomType.UBYTE &&  n > UBYTE_MAX){
						  throw new CompilerException(line,tokenNum,"ubyte number expected");
					  }else if(type == AtomType.USHORT && n > USHORT_MAX){
						  throw new CompilerException(line,tokenNum,"ushort number expected");
					  }else if(type == AtomType.UINT && n > UINT_MAX){
						  throw new CompilerException(line,tokenNum,"uint number expected");
					  }
				  }
			  }
			  if(hex){
			  	 value = "0x" + value;
			  }
			  return new Atom(line,tokenNum,value,type);
		  } else if( c == '{'){
			  return new Atom(line,tokenNum,"{",AtomType.BEGIN_BLOCK);
		  }else if(c == '}'){
			  return new Atom(line,tokenNum,"}",AtomType.END_BLOCK);
		  }else if(c == '\n'){
			 return new Atom(line,tokenNum,"",AtomType.NEW_LINE) ;
		  }else if(c == ','){
			  return new Atom(line,tokenNum,",",AtomType.ARG_SEPARATOR);
		  }else if(c == ';'){
			  return new Atom(line,tokenNum,";",AtomType.END_ARGS);
		  }else  if(c == '"'){
			  var b = new StringBuilder("\"");
			  while(hasNext() && read() != '"'){
			  	if(c == '\n'){
			  		throw new CompilerException(line,tokenNum,"expected \"");
				}
			  	if(c == '\\'){
			  		b.append('\\');
			  		if(!hasNext()){
			  			throw new CompilerException(line,tokenNum,"special character ");
					}
			  		read();//appended in following lines
				}
			  	if(c > 127){
			  		throw new CompilerException(line,tokenNum,"not valid ascii string");
				}
			  	b.appendCodePoint(c);
			  }
			  b.append('"');
			  return new Atom(line,tokenNum,b.toString(),AtomType.STRING);
		  }else if(c == ':'){
		      return new Atom(line,tokenNum,":",AtomType.CLASS_SELECTOR);
          }else  if(notSeparator(c)){
			  var b = new StringBuilder();
			  readNext = false;
			  if(!Character.isValidCodePoint(c)){
			      forceEnd = true;
			      return new Atom(line,tokenNum,"",AtomType.NEW_LINE);
              }
			  while(hasNext() && notSeparator(read())){
				  b.appendCodePoint(c);
			  }
			  return new Atom(line,tokenNum,b.toString(),AtomType.ID);
		  }
		  return null;
	  }
	  public Atom next()throws IOException,CompilerException{
	      if(current != null){
	          var r = current;
	          current = null;
	          return r;
          }
		  Atom token;
		  //noinspection StatementWithEmptyBody
		  while((token = nextPrivate())  == null || token.getType() != AtomType.NEW_LINE && (token.getValue().isEmpty() || token.getValue().isBlank()));
		  if(token.getType() != AtomType.NEW_LINE){
			  tokenNum++;
		  }
		  return token;
	  }
	  private int read()throws IOException{
		  if(readNext){
			 c = reader.read();
			 if(c == '\n'){
				 line++;
				 tokenNum = 1;
			 }
		  }else{
			  readNext = true;
		  }
		  return c;
	  }
	  private boolean notSeparator(int c){
          if(c == '#' || c == '{' || c == ',' || c == ';' || c == '(' || c == '\n' || c == ':' || c == '}'){
              readNext = false;
              return false;
          }
		  return !Character.isWhitespace(c);
	  }
	  public Atom peek()throws IOException,CompilerException{
	      if(current == null){
	          current = next();
          }
	      return current;
      }
}