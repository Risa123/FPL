package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

import java.util.Arrays;

public final class Constructor extends AFunctionBlock{
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum);
        var cEnv = (ClassEnv)env;
        var modEnv = cEnv.getModule();
        if(modEnv.notClassConstructorOnLine(line)){
            modEnv.addClassConstructorLine(line);
        }
        var type = cEnv.getInstanceInfo();
        var constructor = type.getConstructor();
        var fnEnv = new ConstructorEnv(env);
        var b = new StringBuilder();
        var argsCode = new StringBuilder();
        var args = parseArguments(argsCode,it,fnEnv,type);
        var argsArray = args.values().toArray(new TypeInfo[0]);
        if(constructor.hasVariant(argsArray) && modEnv.notClassConstructorOnLine(line)){
            throw new CompilerException(line,tokenNum,"this class already has constructor with arguments " + Arrays.toString(argsArray));
        }
        var hasParentConstructor = false;
        var parentConstructorCall = "";
        if(it.peek() instanceof Atom a && a.getType() == AtomType.CLASS_SELECTOR){
            var callStart = it.next();
            var parentType = (InstanceInfo)type.getPrimaryParent();
            if(parentType == null){
                throw new CompilerException(callStart,"this type has no parent");
            }
            var parentConstructorCallBuilder = new StringBuilder();
            parentType.getConstructor().compileAsParentConstructor(parentConstructorCallBuilder,fnEnv,it,callStart.getLine(),callStart.getTokenNum());
            parentConstructorCallBuilder.append(";\n");
            hasParentConstructor = true;
            parentConstructorCall = parentConstructorCallBuilder.toString();
            cEnv.parentConstructorCalled();
        }
        if(it.hasNext()){
           it.nextList().compile(b,fnEnv,it);
        }else if(!hasParentConstructor){
            throw new CompilerException(line,tokenNum,"block expected as last argument");
        }
        if(!(type instanceof TemplateTypeInfo)){
            cEnv.addConstructor(b.toString(),argsCode.toString(),argsArray,parentConstructorCall);
        }
        for(var field:type.getFields().values()){
            if(field instanceof Variable v && v.getType().isPrimitive() && v.isConstant() && !v.getId().equals("getClass") && !fnEnv.getDefinedConstFields().contains(v.getId())){
              throw new CompilerException(line,tokenNum,"constant field " + v.getId() + " not defined in this constructor");
            }
        }
        return TypeInfo.VOID;
    }
}