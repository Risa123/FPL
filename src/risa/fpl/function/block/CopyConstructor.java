package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class CopyConstructor extends ABlock{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum);
        if(!(env instanceof ClassEnv cEnv)){
            throw new CompilerException(line,tokenNum,"can only be declared in class block");
        }
        if(cEnv.isCopyConstructorDeclared()){
            throw new CompilerException(line,tokenNum,"copy constructor already declared");
        }
        var b = new StringBuilder("void ");
        var type = cEnv.getInstanceInfo();
        var copyName = INTERNAL_METHOD_PREFIX + cEnv.getNameSpace() + "_copy";
        type.setCopyConstructorName(copyName);
        b.append(copyName).append('(').append(type.getCname()).append("* this,").append(type.getCname()).append("* o){\n");
        var parent = (InstanceInfo)type.getPrimaryParent();
        if(parent != null){
            b.append(parent.getDestructorName()).append("(this,o);\n");
        }
        b.append("this->objectData=&").append(type.getDataName()).append(";\n");
        var fnEnv = new FnEnv(env,TypeInfo.VOID);
        fnEnv.addFunction("o",new Variable(new PointerInfo(type),"o","o"));
        fnEnv.addFunction("this",new Variable(new PointerInfo(type),"this","this"));
        b.append(cEnv.getImplicitCopyConstructorCode());
        fnEnv.compileFunctionBlock(b,it);
        b.append("}\n");
        cEnv.appendFunctionCode(b.toString());
        cEnv.declareCopyConstructor();
        return TypeInfo.VOID;
    }
}