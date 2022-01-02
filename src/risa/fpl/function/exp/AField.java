package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

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
    public final void writePrev(StringBuilder builder){
        if(prevCode != null){
            builder.append(prevCode);
            prevCode = null;
        }
    }
    public final String getPrevCode(){
        return prevCode;
    }
    @Override
    public final boolean appendSemicolon(){
        return true;
    }
    @Override
    public final AccessModifier getAccessModifier(){
        return accessModifier;
    }
    public final TypeInfo compileChainedCall(TypeInfo returnType,StringBuilder builder,SubEnv env,ExpIterator it,String prevCode)throws CompilerException{
         if(it.hasNext() && it.peek() instanceof Atom id && id.getType() == AtomType.ID){
           it.next();
           var field = returnType.getField(id.getValue(),env);
           if(field == null){
               throw new CompilerException(id,returnType + " has no field called " + id);
           }
           field.setPrevCode(prevCode);
           return field.compile(builder,env,it,id.getLine(),id.getTokenNum());
         }
         builder.append(prevCode);
         return returnType;
    }
}