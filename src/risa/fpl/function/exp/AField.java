package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

import java.util.Stack;

public abstract class AField implements IFunction{
    protected final Stack<String>prevCodes = new Stack<>();
    protected final AccessModifier accessModifier;
    public AField(AccessModifier accessModifier){
        this.accessModifier = accessModifier;
    }
    public AField(){
        this(AccessModifier.PUBLIC);
    }
    public final void setPrevCode(String code){
        prevCodes.push(code);
    }
    public void writePrev(StringBuilder builder){
        if(getPrevCode() != null){
            builder.append(prevCodes.pop());
        }
    }
    public final String getPrevCode(){
        return prevCodes.empty()?null:prevCodes.peek();
    }
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