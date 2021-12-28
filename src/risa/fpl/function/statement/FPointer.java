package risa.fpl.function.statement;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.block.AFunctionBlock;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.info.FunctionPointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class FPointer extends AFunctionBlock{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum);
        var returnType = env.getType(it.nextID());
        var id = it.nextID();
        var cID = IFunction.toCId(id.getValue());
        var args = parseArguments(new StringBuilder(),it,new FnEnv(env,returnType),null);
        var f = new Function(id.getValue(),returnType,FunctionType.NORMAL,null,env.getAccessModifier(),"");
        f.addVariant(args.values().toArray(new TypeInfo[0]),cID,cID);
        env.addType(new FunctionPointerInfo(f));
        return TypeInfo.VOID;
    }
    @Override
    public boolean appendSemicolon(){
        return true;
    }
}