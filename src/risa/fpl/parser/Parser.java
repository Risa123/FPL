package risa.fpl.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import risa.fpl.CompilerException;

public final class Parser implements AutoCloseable{
 private final Tokenizer tokenizer;
 public Parser(Reader reader){
	 tokenizer = new Tokenizer(reader);
 }
 @Override
 public void close()throws IOException{
	 tokenizer.close();
 }
 public boolean hasNext()throws IOException{
	 return tokenizer.hasNext();
 }
 public List parse()throws IOException,CompilerException{
	 var list = new ArrayList<AExp>();
	 while(hasNext()){
		 list.add(parseStatement(tokenizer.next()));
	 }
	 return new List(1,1,list,false);
 }
 private List parseStatement(Atom first)throws IOException,CompilerException{
	 var list = new ArrayList<AExp>();
	 if(first.getType() != AtomType.NEW_LINE){
		 list.add(first);
	 }
	 while(hasNext()){
		 var token = tokenizer.next();
		 if(token.getType() == AtomType.NEW_LINE || token.getType() == AtomType.END_BLOCK){
			 break;
		 }else if(token.getType() == AtomType.BEGIN_BLOCK){
			 list.add(parseBlock(token));
		 }else{
			 list.add(token);
		 }
	 }
	 return new List(first.getLine(),first.getTokenNum(),list,true);
 }
 private List parseBlock(Atom begin)throws IOException,CompilerException{
	 var list = new ArrayList<AExp>();
	 while(hasNext()){
		 var token = tokenizer.next();
		 if(token.getType() == AtomType.END_BLOCK){
			 break;
		 }else if(token.getType() == AtomType.BEGIN_BLOCK){
			 list.add(parseBlock(token));
		 }else if(token.getType() != AtomType.NEW_LINE){
			 list.add(parseStatement(token));
		 }
	 }
	 return new List(begin.getLine(),begin.getTokenNum(),list,false);
 }
}