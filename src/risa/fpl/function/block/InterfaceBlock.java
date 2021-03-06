package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.InterfaceEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class InterfaceBlock implements IFunction {
    @Override
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        if(!(env instanceof ModuleEnv)){
            throw new CompilerException(line,charNum,"interface can only be declared on module level");
        }
        var id = it.nextID();
        var idV = id.getValue();
        if(env.hasTypeInCurrentEnv(idV)){
            throw new CompilerException(id,"this type " + idV + " is already declared");
        }
        var cID = IFunction.toCId(idV);
        var iEnv = new InterfaceEnv(env,idV);
        var type = iEnv.getType();
        List block = null;
        while(it.hasNext()){
            var exp = it.next();
            if(exp instanceof List l){
                block = l;
                break;
            }else{
               var typeID = (Atom)exp;
               if(typeID.getType() != TokenType.ID){
                   throw new CompilerException(id,"identifier expected");
               }
               var parentType = env.getType(typeID);
               if(!(parentType instanceof InterfaceInfo)){
                   throw new CompilerException(typeID,"interface can only inherit from interfaces");
               }
               type.addParent(parentType);
            }
        }
        if(block == null){
            throw new CompilerException(line,charNum,"block expected as last argument");
        }
        var b = new BuilderWriter(writer);
        var implName = type.getImplName();
        b.write("typedef struct ");
        b.write(implName);
        b.write("{\n");
        for(var parent:type.getParents()){
            for(var method:parent.getMethodsOfType(FunctionType.ABSTRACT)){
                b.write(new PointerInfo(method).getFunctionPointerDeclaration(method.getCname()));
                b.write(";\n");
            }
        }
        block.compile(b,iEnv,it);
        b.write('}');
        b.write(implName);
        b.write(";\n");
        b.write("typedef struct ");
        b.write(cID);
        b.write("{\n");
        b.write("void* instance;\n");
        b.write(implName);
        b.write("* impl;\n}");
        b.write(cID);
        b.write(";\n");
        writer.write(b.getCode());
        type.appendToDeclaration(b.getCode());
        type.buildDeclaration();
        env.addType(idV,type);
        return TypeInfo.VOID;
    }
}