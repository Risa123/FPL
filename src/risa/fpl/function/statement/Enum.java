package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.BinaryOperator;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

public final class Enum implements IFunction{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum);
        var a = it.nextID();
        var id = a.getValue();
        if(env.hasFunctionInCurrentEnv(id)){
            throw new CompilerException(a,"there is already function called " + id);
        }
        var type = new TypeInfo(id,"unsigned char");
        type.addField("==",new BinaryOperator(TypeInfo.BOOL,type,"=="));
        type.addField("!=",new BinaryOperator(TypeInfo.BOOL,type,"!="));
        type.addField("ordinal",new ValueExp(NumberInfo.UBYTE,""));
        var c = new ClassInfo(id);
        type.setClassInfo(c);
        var i = 0;
        while(it.hasNext()){
            var t = it.nextAtom();
            if(t.getType() == AtomType.NEW_LINE){
                break;
            }else if(t.getType() == AtomType.ID){
                if(c.getField(t.getValue(),env) != null){
                    throw new CompilerException(t,"value " + t + " is already declared");
                }
                c.addField(t.getValue(),new ValueExp(type,Integer.toString(i++)));
            }else{
                throw new CompilerException(t,"identifier expected");
            }
        }
        env.addType(type);
        return TypeInfo.VOID;
    }
}