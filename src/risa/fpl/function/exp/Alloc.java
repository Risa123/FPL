package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Alloc extends AField{
    private final boolean array;
    private final TypeInfo type,p;
    public Alloc(TypeInfo type,boolean array){
        this.type = type;
        p = new PointerInfo(type);
        this.array = array;
    }
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        builder.append("((").append(p.getCname());
        var notOneByteType = !(type == TypeInfo.CHAR || type == TypeInfo.BOOL || type instanceof NumberInfo n && n.getSize() == 1);
        builder.append(")_std_lang_malloc0(");
        if(notOneByteType){
            builder.append("sizeof(").append(type.getCname()).append(')');
        }
        if(array){
            if(notOneByteType){
                builder.append("*(");
            }
            var count = it.next();
            var returnType = count.compile(builder,env,it);
            if(returnType.notIntegerNumber()){
                error(count,"integer number expected instead of " + returnType);
            }
        }
        if(notOneByteType){
            builder.append(')');
        }
        builder.append("))");
        return compileChainedCall(p,builder,env,it,getPrevCode() == null?"":getPrevCode());
    }
}