package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

import java.io.BufferedWriter;
import java.io.IOException;

public abstract class AField implements IField{
    private String prevCode;
    private final AccessModifier accessModifier;
    public AField(AccessModifier accessModifier){
        this.accessModifier = accessModifier;
    }
    public AField(){
        this(AccessModifier.PUBLIC);
    }
    @Override
    public void setPrevCode(String code){
        prevCode = code;
    }
    @Override
    public void writePrev(BufferedWriter writer)throws IOException{
        if(prevCode != null){
            writer.write(prevCode);
            prevCode = null;
        }
    }
    @Override
    public String getPrevCode(){
        return prevCode;
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
         if(it.hasNext() && it.peek() instanceof Atom id && id.getType() == AtomType.ID){
           it.next();
           var field = returnType.getField(id.getValue(),env);
           if(field == null){
               throw new CompilerException(id,returnType + " has no field called " + id);
           }
           field.setPrevCode(prevCode);
           return field.compile(writer,env,it,id.getLine(),id.getTokenNum());
         }
         writer.write(prevCode);
         return returnType;
    }
}