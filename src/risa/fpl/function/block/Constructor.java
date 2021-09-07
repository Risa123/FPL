package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

public final class Constructor extends AFunctionBlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var cEnv = (ClassEnv)env;
        var modEnv = (ModuleEnv)cEnv.getSuperEnv();
        if(modEnv.notClassConstructorOnLine(line)){
            modEnv.addClassConstructorLine(line);
        }
        var type = cEnv.getInstanceType();
        var constructor = type.getConstructor();
        if(constructor == null){
            constructor = new ClassVariable(cEnv.getInstanceType(),cEnv.getClassType());
            type.setConstructor(constructor);
            cEnv.getSuperEnv().addFunction(type.getName(),constructor);
        }
        var fEnv = new FnEnv(env,TypeInfo.VOID);
        var b = new BuilderWriter();
        b.write("void " + INTERNAL_METHOD_PREFIX + cEnv.getNameSpace(this) + "_init");
        var variantNum = constructor.getVariants().size();
        var argsWriter = new BuilderWriter();
        var args = parseArguments(argsWriter,it,fEnv,type).values().toArray(new TypeInfo[0]);
        if(constructor.hasVariant(args)){
          if(modEnv.notClassConstructorOnLine(line)){
              throw new CompilerException(line,tokenNum,"this class already has constructor with arguments " + Arrays.toString(args));
          }
          variantNum--;
        }
        b.write(Integer.toString(variantNum));
        if(!(type instanceof TemplateTypeInfo)){
            constructor.addVariant(args,cEnv.getNameSpace());
        }
        b.write(argsWriter.getCode() + "{\n");
        var hasParentConstructor = false;
        if(it.peek() instanceof Atom a && a.getType() == TokenType.CLASS_SELECTOR){
            var callStart = it.next();
            var parentType = (InstanceInfo)type.getPrimaryParent();
            if(parentType == null){
                throw new CompilerException(callStart,"this type has no parent");
            }
            parentType.getConstructor().compileAsParentConstructor(b,fEnv,it,callStart.getLine(),callStart.getTokenNum());
            b.write(";\n");
            hasParentConstructor = true;
            cEnv.parentConstructorCalled();
        }
        b.write(cEnv.getImplicitConstructorCode());
        if(it.hasNext()){
           it.nextList().compile(b,fEnv,it);
        }else if(!hasParentConstructor){
            throw new CompilerException(line,tokenNum,"block expected as last argument");
        }
        b.write("}\n");
        if(!(type instanceof TemplateTypeInfo)){
            cEnv.compileNewAndAlloc(b,args,constructor);
            cEnv.addMethod(constructor,args,b.getCode());
        }
        return TypeInfo.VOID;
    }
}