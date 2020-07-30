package risa.fpl.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.tokenizer.Token;
import risa.fpl.tokenizer.TokenType;
import risa.fpl.tokenizer.Tokenizer;

public final class Parser implements AutoCloseable{
 private final Tokenizer tokenizer;
 public Parser(Reader reader) {
	 tokenizer = new Tokenizer(reader);
 }
 @Override
 public void close() throws IOException {
	 tokenizer.close();
 }
 public boolean hasNext() throws IOException { 
	 return tokenizer.hasNext();
 }
 public List parse() throws IOException, CompilerException {
	 var list = new ArrayList<AExp>();
	 while(hasNext()) {
		 var token = tokenizer.next();
		 list.add(parseStatement(token));
	 }
	 return new List(1,1,list,false);
 }
 private List parseStatement(Token first) throws IOException, CompilerException {
	 var list = new ArrayList<AExp>();
	 if(first.type != TokenType.NEW_LINE) {
		 list.add(new Atom(first.line,first.charNum,first.value,first.type));
	 }
	 while(hasNext()) {
		 var token = tokenizer.peek();
		 if(token.type == TokenType.NEW_LINE) {
		     tokenizer.next();
			 break;
		 }else if(token.type == TokenType.END_BLOCK) {
		    break;
		 }else if(token.type == TokenType.BEGIN_BLOCK) {
		     tokenizer.next();
			 list.add(parseBlock(token));
		 }else {
             tokenizer.next();
			 list.add(new Atom(token.line,token.charNum,token.value,token.type));
		 }
	 }
	 return new List(first.line,first.charNum,list,true);
 }
 private List parseBlock(Token begin) throws IOException, CompilerException {
	 var list = new ArrayList<AExp>();
	 while(hasNext()) {
		 var token = tokenizer.next();
		 if(token.type == TokenType.END_BLOCK) {
			 break;
		 }else if(token.type == TokenType.BEGIN_BLOCK) {
			 list.add(parseBlock(token));
		 }else if(token.type  != TokenType.NEW_LINE) {
			 list.add(parseStatement(token));
		 }
	 }
	 return new List(begin.line,begin.charNum,list,false);
 }
}