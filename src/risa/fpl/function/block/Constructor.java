package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

public final class Constructor extends AFunctionBlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var cEnv = (ClassEnv)env;
        var modEnv = (ModuleEnv)cEnv.getSuperEnv();
        if(modEnv.notClassConstructorOnLine(line)){
            modEnv.addClassConstructorLine(line);
        }
        var type = cEnv.getInstanceType();
        var constructor = type.getConstructor();
        var fnEnv = new ConstructorEnv(env);
        var b = new BuilderWriter();
        b.write("void " + INTERNAL_METHOD_PREFIX + cEnv.getNameSpace(this) + "_init");
        var variantNum = constructor.getVariants().size();
        var argsWriter = new BuilderWriter();
        var args = parseArguments(argsWriter,it,fnEnv,type).values().toArray(new TypeInfo[0]);
        if(constructor.hasVariant(args) && modEnv.notClassConstructorOnLine(line)){
            throw new CompilerException(line,tokenNum,"this class already has constructor with arguments " + Arrays.toString(args));
        }
        if(cEnv.hasOnlyImplicitConstructor()){
            variantNum--;
        }
        b.write(Integer.toString(variantNum));
        b.write(argsWriter.getCode() + "{\n");
        var hasParentConstructor = false;
        if(it.peek() instanceof Atom a && a.getType() == AtomType.CLASS_SELECTOR){
            var callStart = it.next();
            var parentType = (InstanceInfo)type.getPrimaryParent();
            if(parentType == null){
                throw new CompilerException(callStart,"this type has no parent");
            }
            parentType.getConstructor().compileAsParentConstructor(b,fnEnv,it,callStart.getLine(),callStart.getTokenNum());
            b.write(";\n");
            hasParentConstructor = true;
            cEnv.parentConstructorCalled();
        }
        b.write(cEnv.getImplicitConstructorCode());
        if(it.hasNext()){
           it.nextList().compile(b,fnEnv,it);
        }else if(!hasParentConstructor){
            throw new CompilerException(line,tokenNum,"block expected as last argument");
        }
        b.write("}\n");
        if(!(type instanceof TemplateTypeInfo)){
            cEnv.addConstructor(b.getCode(),args);
        }
        for(var field:type.getFields().values()){
            if(field instanceof Variable v && v.getType().isPrimitive() && v.isConstant() && !v.getId().equals("getClass") && !fnEnv.getDefinedConstFields().contains(v.getId())){
              throw new CompilerException(line,tokenNum,"constant field " + v.getId() + " not defined in this constructor");
            }
        }
        return TypeInfo.VOID;
    }
}