package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.FnSubEnv;
import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.AtomType;
import risa.fpl.parser.ExpIterator;

public final class ForLoop extends ABlock{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum);
        builder.append("for(");
        var firstAtom = it.nextID();
        var secondAtom = it.nextAtom();
        Atom id;
        TypeInfo type = null;
        if(secondAtom.getType() == AtomType.ARG_SEPARATOR){
            id = firstAtom;
        }else if(secondAtom.getType() == AtomType.ID){
           id = secondAtom;
           type = env.getType(firstAtom);
           if(type.notIntegerNumber()){
               throw new CompilerException(firstAtom,"integer type expected");
           }
           var sep = it.nextAtom();
           if(sep.getType() != AtomType.ARG_SEPARATOR){
               throw new CompilerException(sep,", expected");
           }
        }else{
            throw new CompilerException(secondAtom,", or identifier expected");
        }
        var b = new StringBuilder();
        var expAtom = it.nextAtom();
        var expType = expAtom.compile(b,env,it);
        if(expType.notIntegerNumber()){
            throw new CompilerException(expAtom,"expression returning integer number expected");
        }
        if(type == null){
            type = expType;
        }
        var cId = IFunction.toCId(id.getValue());
        builder.append(type.getCname()).append(' ').append(cId).append("=0;");
        builder.append(cId).append('<').append(b).append(';');
        builder.append(cId).append("++){\n");
        var subEnv = new FnSubEnv(env);
        subEnv.addFunction(id.getValue(),new Variable(type,cId,id.getValue()));
        var tmp = new StringBuilder();
        it.nextList().compile(tmp,subEnv,it);
        subEnv.compileToPointerVars(builder);
        builder.append(tmp);
        subEnv.compileDestructorCalls(builder);
        builder.append("}\n");
        return TypeInfo.VOID;
    }
}