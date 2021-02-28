package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public abstract class AField implements IField{
    private String prev_code;
    private final AccessModifier accessModifier;
    public AField(AccessModifier accessModifier){
        this.accessModifier = accessModifier;
    }
    public AField(){
        this(AccessModifier.PUBLIC);
    }
    @Override
    public void setPrevCode(String code){
        prev_code = code;
    }
    @Override
    public void writePrev(BufferedWriter writer)throws IOException{
        if(prev_code != null){
            writer.write(prev_code);
            prev_code = null;
        }
    }
    @Override
    public String getPrevCode(){
        return prev_code;
    }
    @Override
    public boolean appendSemicolon(){
        return true;
    }
    @Override
    public AccessModifier getAccessModifier(){
        return accessModifier;
    }
    public TypeInfo compileChainedCall(TypeInfo returnType,BufferedWriter writer,AEnv env,ExpIterator it,String prevCode)throws IOException,CompilerException{
         if(it.hasNext() && it.peek() instanceof Atom id && id.getType() == TokenType.ID){
           it.next();
           var field = returnType.getField(id.getValue(),env);
           if(field == null){
               throw new CompilerException(id,returnType + " has not field called " + id);
           }
           field.setPrevCode(prevCode);
           return field.compile(writer,env,it,id.getLine(),id.getCharNum());
         }
         writer.write(prevCode);
         return returnType;
    }
}