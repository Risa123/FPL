package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Destructor extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        if(!(env instanceof ClassEnv cEnv)){
            throw new CompilerException(line,tokenNum,"can only be declared in class block");
        }
        if(cEnv.isDestructorDeclared()){
            throw new CompilerException(line,tokenNum,"destructor already declared");
        }
        var b = new BuilderWriter();
        b.write("void ");
        var type = cEnv.getInstanceInfo();
        type.setDestructorName(INTERNAL_METHOD_PREFIX + cEnv.getNameSpace());
        b.write(type.getDestructorName() + "(" + type.getCname() + "* this){\n");
        var parent = (InstanceInfo)type.getPrimaryParent();
        if(parent != null){
            b.write(parent.getDestructorName() + "(this);\n");
        }
        b.write(cEnv.getImplicitDestructorCode());
        it.nextList().compile(b,new FnEnv(env,TypeInfo.VOID),it);
        b.write("}\n");
        cEnv.appendFunctionCode(b.getCode());
        cEnv.destructorDeclared();
        return TypeInfo.VOID;
    }
}