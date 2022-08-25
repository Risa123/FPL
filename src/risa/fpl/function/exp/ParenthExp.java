package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.*;

import java.util.ArrayList;

public final class ParenthExp extends AField{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        var list = new ArrayList<AExp>();
        var notIn = true;
        while(it.hasNext()){
            var exp = it.nextAtom();
            if(exp.getType() == AtomType.ID){
                if(exp.getValue().equals("[")){
                    notIn = false;
                }else if(exp.getValue().equals("]")){
                    if(notIn){
                        break;
                    }else{
                        notIn = true;
                    }
                }
            }
            list.add(exp);
        }
        var b = new StringBuilder("(");
        var ret = new List(line,tokenNum,list,false).compile(b,env,it);
        if(b.charAt(b.length() - 1) == '\n'){
            b.delete(b.length() - 1,b.length() - 2);
        }
        b.append(')');
        if(ret != TypeInfo.VOID && it.hasNext() && it.peek() instanceof Atom a && a.getType() == AtomType.ID){
           var id = it.nextID();
           var field = ret.getField(id.getValue(),env);
           if(field == null){
               throw new CompilerException(id,ret + " has no field called " + id);
           }
           var code = b.toString();
           if(field instanceof Function f && ret instanceof InstanceInfo i){
              code = i.getToPointerName() + '(' + code + ",&" + env.getToPointerVarName(i);
              f.calledOnInstanceRByFunc();
           }
           field.setPrevCode(code);
           return field.compile(builder,env,it,line,tokenNum);
        }
        builder.append(b);
        return ret;
    }
}