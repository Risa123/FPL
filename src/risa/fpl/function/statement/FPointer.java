package risa.fpl.function.statement;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.block.AFunctionBlock;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class FPointer extends AFunctionBlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var returnType = env.getType(it.nextID());
        var id = it.nextID();
        var cID = IFunction.toCId(id.getValue());
        var args = parseArguments(new BuilderWriter(writer),it,new FnEnv(env,returnType),null);
        var f = new Function(id.getValue(),returnType,cID,args.values().toArray(new TypeInfo[0]), FunctionType.NORMAL,null,env.getAccessModifier(),"");
        env.addType(id.getValue(),new PointerInfo(f));
        return TypeInfo.VOID;
    }
    @Override
    public boolean appendSemicolon(){
        return true;
    }
}