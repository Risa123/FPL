package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.StructEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class StructBlock extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var id = it.nextID();
        var cID = IFunction.toCId(id.getValue());
        if(env.hasTypeInCurrentEnv(id.getValue())){
            throw new CompilerException(id,"type " + id +  " is already declared");
        }
        var b  = new BuilderWriter(writer);
        var next = it.next();
        var align = "";
        if(next instanceof Atom a){
            if(!a.getValue().equals("align")){
                throw new CompilerException(a,"align expected");
            }
            var arg = it.next();
            var alignValue = "";
            if(arg instanceof Atom argAtom){
                if(argAtom.notIndexLiteral()){
                    throw new CompilerException(arg,"integer number expected");
                }
                alignValue = "(" + argAtom +  ")";
                next = it.nextList();
            }else{
               next = arg;
            }
            align = " __attribute__((aligned" + alignValue + "))";
        }
        b.write("typedef struct " + cID + "{\n");
        var sEnv = new StructEnv(env,cID);
        next.compile(b,sEnv,it);
        b.write("}" + cID +  align +";\n");
        var type = sEnv.getType();
        writer.write(b.getCode());
        type.appendToDeclaration(b.getCode());
        type.buildDeclaration();
        return TypeInfo.VOID;
    }
}