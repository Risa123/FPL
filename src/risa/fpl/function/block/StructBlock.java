package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.StructEnv;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class StructBlock extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var id = it.nextID();
        if(env.hasTypeInCurrentEnv(id.getValue())){
            throw new CompilerException(id,"type " + id +  " is already declared");
        }
        var sEnv = new StructEnv(env,id.getValue());
        var cID = sEnv.getType().getCname();
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
        next.compile(b,sEnv,it);
        b.write("}" + cID +  align +";\n");
        var type = sEnv.getType();
        type.appendToDeclaration(b.getCode());
        type.buildDeclaration();
        env.addType(id.getValue(),type);
        return TypeInfo.VOID;
    }
}