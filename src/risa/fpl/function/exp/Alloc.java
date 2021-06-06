package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Alloc extends AField{
    private final boolean array;
    private final TypeInfo type,p;
    public Alloc(TypeInfo type,boolean array){
        this.type = type;
        p = new PointerInfo(type);
        this.array = array;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        writer.write("((");
        writer.write(p.getCname());
        var notOneByteType = type != TypeInfo.CHAR ||!(type instanceof NumberInfo n && n.getSize() == 1);
        writer.write(")_std_lang_malloc0(");
        if(notOneByteType){
            writer.write("sizeof(");
            writer.write(type.getCname());
            writer.write(')');
        }
        if(array){
            if(notOneByteType){
                writer.write("*(");
            }
            var count = it.next();
            var returnType = count.compile(writer,env,it);
            if(returnType.notIntegerNumber()){
                throw new CompilerException(count,"integer number expected instead of " + returnType);
            }
        }
        if(notOneByteType){
            writer.write(')');
        }
        writer.write("))");
        return compileChainedCall(p,writer,env,it,getPrevCode() == null?"":getPrevCode());
    }
}