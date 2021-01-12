package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.CStructEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class CStructBlock extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var id = it.nextID();
        var cID = id.getValue();
        if(env.hasTypeInCurrentEnv(id.getValue())){
            throw new CompilerException(id,"this type is already declared");
        }
        if(IFunction.notCID(cID)){
            throw new CompilerException(id,"invalid C identifier");
        }
        var b  = new BuilderWriter(writer);
        var next = it.next();
        var align = "";
        if(next instanceof Atom a){
            if(!a.getValue().equals("align")){
                throw new CompilerException(a,"align expected");
            }
            var arg = it.nextAtom();
            if(arg.getType() != TokenType.UINT && arg.getType() != TokenType.ULONG){
                throw new CompilerException(arg,"integer number expected");
            }
            next = it.nextList();
            align = " __attribute__((aligned(" + arg.getValue() + ")))";
        }
        b.write("typedef struct " + cID + "{\n");
        var sEnv = new CStructEnv(env,cID);
        next.compile(b,sEnv,it);
        b.write("}" + cID +  align +";\n");
        var type = sEnv.getType();
        writer.write(b.getCode());
        type.appendToDeclaration(b.getCode());
        type.buildDeclaration();
        return TypeInfo.VOID;
    }
}