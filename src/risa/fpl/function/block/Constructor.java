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
        var type = cEnv.getInstanceInfo();
        var constructor = type.getConstructor();
        var fnEnv = new ConstructorEnv(env);
        var b = new StringBuilder();
        var argsCode = new StringBuilder();
        var args = parseArguments(argsCode,it,fnEnv,type);
        var argsArray = args.values().toArray(new TypeInfo[0]);
        cEnv.removeImplicitConstructor();
        if(constructor.hasVariant(argsArray) && constructor.getVariant(argsArray).getLine() != line){
            error(line,tokenNum,"this class already has constructor with arguments " + Arrays.toString(argsArray));
        }
        var calledAnotherConstructor = false;
        var callToAnotherConstructor = "";
        var parentType = (InstanceInfo)type.getPrimaryParent();
        if(it.peek() instanceof Atom a && a.getType() == AtomType.CLASS_SELECTOR){
            it.next();
            var constructorOwner = it.nextID();
            var callToAnotherConstructorBuilder = new StringBuilder();
            if(constructorOwner.getValue().equals("this")){
               calledAnotherConstructor = true;
               type.getConstructor().compileCallInsideOfConstructor(callToAnotherConstructorBuilder,fnEnv,it,constructorOwner.getLine(),constructorOwner.getTokenNum());
               callToAnotherConstructorBuilder.append(";\n");
               callToAnotherConstructor = callToAnotherConstructorBuilder.toString();
            }else if(constructorOwner.getValue().equals("super")){
                if(parentType == null){
                    throw new CompilerException(constructorOwner,"this type has no parent");
                }
                parentType.getConstructor().compileCallInsideOfConstructor(callToAnotherConstructorBuilder,fnEnv,it,constructorOwner.getLine(),constructorOwner.getTokenNum());
                callToAnotherConstructorBuilder.append(";\n");
                calledAnotherConstructor = true;
                callToAnotherConstructor = callToAnotherConstructorBuilder.toString();
                cEnv.parentConstructorCalled();
            }else{
                error(constructorOwner,"super or this expected");
            }
        }else if(parentType != null){
           var parentConstructor = parentType.getConstructor();
           if(!parentConstructor.hasVariant(new TypeInfo[0])){
               error(line,tokenNum,"parent does not have implicit constructor");
           }
           callToAnotherConstructor = parentConstructor.getVariant(new TypeInfo[0]).getCname() + "((" + parentType.getCname() + "*)this);\n";
        }
        if(it.hasNext()){
           fnEnv.compileFunctionBlock(b,it);
        }else if(!calledAnotherConstructor){
            error(line,tokenNum,"block expected as last argument");
        }
        if(!(type instanceof TemplateTypeInfo)){
            cEnv.addConstructor(b.toString(),argsCode.toString(),argsArray,callToAnotherConstructor,line);
        }
        for(var field:type.getFields().values()){
            if(field instanceof Variable v && v.getType().isPrimitive() && v.isConstant() && !v.getId().equals("getClass") && !fnEnv.getDefinedConstFields().contains(v.getId())){
              error(line,tokenNum,"constant field " + v.getId() + " not defined in this constructor");
            }
        }
        return TypeInfo.VOID;
    }
}