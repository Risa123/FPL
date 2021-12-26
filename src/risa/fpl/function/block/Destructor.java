package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Destructor extends ABlock{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        if(!(env instanceof ClassEnv cEnv)){
            throw new CompilerException(line,tokenNum,"can only be declared in class block");
        }
        if(cEnv.isDestructorDeclared()){
            throw new CompilerException(line,tokenNum,"destructor already declared");
        }
        var b = new StringBuilder("void ");
        var type = cEnv.getInstanceInfo();
        type.setDestructorName(INTERNAL_METHOD_PREFIX + cEnv.getNameSpace());
        b.append(type.getDestructorName()).append("(").append(type.getCname()).append("* this){\n");
        var parent = (InstanceInfo)type.getPrimaryParent();
        if(parent != null){
            b.append(parent.getDestructorName()).append("(this);\n");
        }
        b.append(cEnv.getImplicitDestructorCode());
        it.nextList().compile(b,new FnEnv(env,TypeInfo.VOID),it);
        b.append("}\n");
        cEnv.appendFunctionCode(b.toString());
        cEnv.destructorDeclared();
        return TypeInfo.VOID;
    }
}