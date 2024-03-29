package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.CustomTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Typedef implements IFunction{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum);
        builder.append("typedef ");
        var type = it.nextID();
        if(env.hasTypeInCurrentEnv(type.getValue()) && env.getType(type) instanceof CustomTypeInfo t && t.isPrimitive()){
            error(type,"type " + type + " is already defined");
        }
        if(env.hasFunctionInCurrentEnv(type.getValue())){
            error(type,"type cannot be declared identifier for declaration function occupied");
        }
        var originalType = it.nextID();
        var after = "";
        if(originalType.getValue().equals("[]")){
           var len = it.nextAtom();
           if(len.notIndexLiteral()){
               error(len,"array length expected instead of " + len);
           }
           after = "[" + len + ']';
           originalType = it.nextID();
        }
        var t = env.getType(originalType);
        if(it.checkTemplate()){
           t =  generateTypeFor(t,originalType,it,env,false);
        }
        if(t == null){
            throw new CompilerException(originalType,"type " + originalType + " not found");
        }
        if(!t.isPrimitive()){
            error(originalType,"primitive type expected");
        }
        var b = new StringBuilder(t.getCname()).append(' ').append(IFunction.toCId(type.getValue())).append(after);
        builder.append(b);
        env.addType(new CustomTypeInfo(type.getValue(),t,b.toString()));
        return TypeInfo.VOID;
    }
}
