package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.InterfaceEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.info.FunctionInfo;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class InterfaceBlock implements IFunction{
    @Override
    public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        if(!(env instanceof ModuleEnv mod)){
            throw new CompilerException(line,tokenNum,"interface can only be declared on module level");
        }
        var id = it.nextID();
        var idV = id.getValue();
        if(env.hasTypeInCurrentEnv(idV)){
            throw new CompilerException(id,"this type " + idV + " is already declared");
        }
        var cID = IFunction.toCId(idV);
        var iEnv = new InterfaceEnv(mod,idV);
        var type = iEnv.getType();
        List block = null;
        while(it.hasNext()){
            var exp = it.next();
            if(exp instanceof List l){
                block = l;
                break;
            }else{
               var typeID = (Atom)exp;
               if(typeID.getType() != AtomType.ID){
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
            throw new CompilerException(line,tokenNum,"block expected as last argument");
        }
        block.compile(new BuilderWriter(),iEnv,it);
        var implName = type.getImplName();
        var b = new BuilderWriter();
        b.write("typedef struct ");
        b.write(implName);
        b.write("{\n");
        for(var method:type.getMethodsOfType(FunctionType.ABSTRACT)){
            var p = new FunctionInfo(method);
            for(var v:method.getVariants()){
                b.write(p.getPointerVariableDeclaration(v.cname()));
                b.write(";\n");
            }
        }
        b.write('}' + implName + ";\n");
        b.write("typedef struct " + cID + "{\n");
        b.write("void* instance;\n");
        b.write(implName + "* impl;\n}" + cID + ";\n");
        type.appendToDeclaration(b.getCode());
        type.buildDeclaration();
        env.addType(type);
        return TypeInfo.VOID;
    }
}