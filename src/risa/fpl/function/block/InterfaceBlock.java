package risa.fpl.function.block;

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

public final class InterfaceBlock implements IFunction{
    @SuppressWarnings("ConstantConditions")
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        if(!(env instanceof ModuleEnv mod)){
            throw new CompilerException(line,tokenNum,"interface can only be declared on module level");
        }
        env.checkModifiers(line,tokenNum);
        var id = it.nextID();
        var idV = id.getValue();
        if(env.hasTypeInCurrentEnv(idV)){
            throw new CompilerException(id,"this type " + idV + " is already declared");
        }
        var cID = IFunction.toCId(idV);
        var iEnv = new InterfaceEnv(mod,idV);
        var type = iEnv.getType();
        env.addType(type);
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
               if(!(env.getType(typeID) instanceof InterfaceInfo parentType)){
                   throw new CompilerException(typeID,"interface can only inherit from interfaces");
               }
               type.addParent(parentType);
            }
        }
        if(block == null){
            throw new CompilerException(line,tokenNum,"block expected as last argument");
        }
        block.compile(new StringBuilder(),iEnv,it);
        var implName = type.getImplName();
        var b = new StringBuilder("typedef struct ").append(implName).append("{\n");
        for(var method:type.getMethodsOfType(FunctionType.ABSTRACT)){
            var p = new FunctionInfo(method);
            for(var v:method.getVariants()){
                b.append(p.getPointerVariableDeclaration(v.getCname())).append(";\n");
            }
        }
        b.append('}').append(implName).append(";\n");
        b.append("typedef struct ").append(cID).append("{\n");
        b.append("void* instance;\n");
        b.append(implName).append("* impl;\n}").append(cID).append(";\n");
        type.appendToDeclaration(b.toString());
        type.buildDeclaration();
        return TypeInfo.VOID;
    }
}