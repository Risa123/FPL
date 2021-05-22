package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Constructor extends AFunctionBlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var cEnv = (ClassEnv)env;
        var type = cEnv.getInstanceType();
        ClassVariable constructor;
        if(cEnv.getSuperEnv().hasFunctionInCurrentEnv(type.getName()) && cEnv.getSuperEnv().getFunction(type.getName()) instanceof ClassVariable cv){
           constructor = cv;
        }else{
            constructor = new ClassVariable(cEnv.getInstanceType(),cEnv.getClassType());
            type.setConstructor(constructor);
            cEnv.getSuperEnv().addFunction(type.getName(),constructor);
        }
        var fEnv = new FnEnv(env,TypeInfo.VOID);
        var b = new BuilderWriter(writer);
        b.write("void ");
        b.write(INTERNAL_METHOD_PREFIX);
        b.write(cEnv.getNameSpace(this));
        b.write("_init");
        b.write(Integer.toString(constructor.getVariants().size()));
        var args = parseArguments(b,it,fEnv,type).values().toArray(new TypeInfo[0]);
        if(constructor.hasVariant(args)){
            // throw new CompilerException(line,charNum,"this class already has constructor with arguments " + Arrays.toString(args));
        }
        if(!(type instanceof TemplateTypeInfo)){
            constructor.addVariant(args,cEnv.getNameSpace());
        }
        b.write("{\n");
        var hasParentConstructor = false;
        if(it.peek() instanceof Atom a && a.getType() == TokenType.CLASS_SELECTOR){
            var callStart = it.next();
            var parentType = (InstanceInfo)type.getPrimaryParent();
            if(parentType == null){
                throw new CompilerException(callStart,"this type has no parent");
            }
            parentType.getConstructor().compileAsParentConstructor(b,fEnv,it,callStart.getLine(),callStart.getCharNum());
            b.write(";\n");
            hasParentConstructor = true;
            cEnv.parentConstructorCalled();
        }
        b.write(cEnv.getImplicitConstructorCode());
        if(it.hasNext()){
           it.nextList().compile(b,fEnv,it);
        }else if(!hasParentConstructor){
            throw new CompilerException(line,charNum,"block expected as last argument");
        }
        b.write("}\n");
        if(!(type instanceof TemplateTypeInfo)){
            cEnv.compileNewAndAlloc(b,args,constructor);
            cEnv.addMethod(constructor,args,b.getCode());
        }
        return TypeInfo.VOID;
    }
}