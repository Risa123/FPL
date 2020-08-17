package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.Modifier;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public final class ClassBlock extends ATwoPassBlock implements IFunction {

	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		if(!(env instanceof ModuleEnv modEnv)){
		    throw new CompilerException(line,charNum,"can only be used on module level");
        }
        var id = it.nextID();
		var idV = id.getValue();
		if(env.hasTypeInCurrentEnv(idV)){
		    throw new CompilerException(id,"this type is already declared");
        }
        String cID;
        if(env.hasModifier(Modifier.NATIVE)){
            cID = id.getValue();
            if(!IFunction.isCId(cID)){
                throw new CompilerException(id,"invalid C identifier");
            }
        }else{
            cID = IFunction.toCId(id.getValue());
        }
        var cEnv = new ClassEnv(modEnv,cID,idV);
		TypeInfo parentType = null;
		List block = null;
		var interfaces = new ArrayList<InterfaceInfo>();
        while(it.hasNext()){
            var exp = it.next();
            if(exp instanceof List l){
                block = l;
                break;
            }else{
                var typeID = (Atom)exp;
                if(typeID.getType() != TokenType.ID){
                    throw new CompilerException(typeID,"identifier expected");
                }
                var type = env.getType(typeID);
                if(type.isPrimitive()){
                    throw new CompilerException(typeID,"primitive types cannot inherited from");
                }
                cEnv.getInstanceType().addParent(type);
                if(type instanceof InterfaceInfo i){
                   interfaces.add(i);
                }else{
                    if(parentType != null){
                        throw new CompilerException(typeID,"there is already parent class");
                    }
                    parentType = type;
                    cEnv.getInstanceType().setPrimaryParent(parentType,cEnv.getNameSpace());
                }
            }
        }
        if(block == null){
            throw new CompilerException(line,charNum,"block expected as last argument");
        }
		var b = new BuilderWriter(writer);
		b.write("typedef struct ");
	    b.write(IFunction.toCId(id.getValue()));
	    b.write("{\n");
	    b.write("void* class_data;\n");
	    if(parentType != null){
            b.write(parentType.getCname());
            b.write(" parent;\n");
        }
        try{
            compile(b,cEnv,block);
        }catch(CompilerException ex){
            ex.setSourceFile("");
            throw ex;
        }
        //parent type doesn't have implicit constructor
        if(parentType != null && parentType.getConstructor().getArguments().length > 0 && !cEnv.isParentConstructorCalled()){
            throw new CompilerException(line,charNum,"constructor is required to call parent constructor");
        }
	    b.write('}');
	    b.write(IFunction.toCId(id.getValue()));
	    b.write(";\n");
	    b.write(cEnv.getDataDeclaration());
        var type = cEnv.getInstanceType();
        if(!cEnv.isAbstract()){
            for(var method:type.getAbstractMethods()){
                var name = method.getName();
                var impl = type.getField(name,cEnv);
                if(!(impl instanceof Function) || ((Function)impl).getType() == FunctionType.ABSTRACT){
                    throw new CompilerException(line,charNum,"this class doesn't implement method " + name);
                }
            }
        }
        writer.write(b.getCode());
        writer.write(cEnv.getDataDefinition());
        var constructor = type.getConstructor();
        if(constructor == null){
            constructor = new ClassVariable(type,cEnv.getClassType(),new TypeInfo[]{},cEnv.getNameSpace(this),env);
            cEnv.addMethod(constructor,cEnv.getImplicitConstructor());
            type.setConstructor(constructor);
        }
        writer.write(cEnv.getMethodCode());
        var newName = new StringBuilder();
        newName.append(modEnv.getNameSpace(this));
        newName.append(cID);
        newName.append("_new");
        type.appendToDeclaration(b.getCode());
        cEnv.appendDeclarations();
        if(!env.hasModifier(Modifier.ABSTRACT)){
            writer.write(cID);
            writer.write("* ");
            writer.write(newName.toString());
            writer.write('(');
            var args = constructor.getArguments();
            var first = true;
            for(int i = 0; i < args.length;++i){
                if(first){
                    first = false;
                }else{
                    writer.write(',');
                }
                writer.write(args[i].getCname());
                writer.write(" a");
                writer.write(Integer.toString(i));
            }
            writer.write("){\n");
            writer.write("void* malloc(unsigned long);\n");
            writer.write(type.getCname());
            writer.write("* p=malloc(sizeof ");
            writer.write(type.getCname());
            writer.write(");\n");
            writer.write(constructor.getCname());
            writer.write("(p");
            for(int i = 0; i < constructor.getArguments().length;++i){
                writer.write(",a");
                writer.write(Integer.toString(i));
            }
            writer.write(");");
            writer.write("\nreturn p;\n}\n");
            var newMethod = Function.newNew(newName.toString(),type,constructor.getArguments(),env);
            cEnv.getClassType().addField("new",newMethod);
            writer.write(newMethod.getDeclaration());
            type.appendToDeclaration(newMethod.getDeclaration());
        }
	    env.addType(idV,type);
	    if(!env.hasModifier(Modifier.ABSTRACT)){
            env.addFunction(id.getValue(),constructor);
        }
        for(var i:interfaces){
           writer.write("static ");
           writer.write(i.getImplName());
           writer.write(' ');
           writer.write(cID);
           writer.write(i.getCname());
           writer.write("_impl;\n");
           writer.write(i.getCname());
           writer.write(' ');
           var callBuilder = new StringBuilder(INTERNAL_METHOD_PREFIX);
           callBuilder.append(cEnv.getNameSpace());
           callBuilder.append("_as");
           callBuilder.append(i.getCname());
           var asCName = callBuilder.toString();
           writer.write(asCName);
           writer.write('(');
           writer.write(type.getCname());
           writer.write("* this){\n");
           writer.write(i.getCname());
           writer.write(" tmp;\ntmp.instance=this;\n");
           writer.write("tmp.impl=&");
           writer.write(cEnv.getImplOf(i));
           writer.write(";\nreturn tmp;\n}\n");
           type.appendToDeclaration(i.getCname() + " " + callBuilder + "(" + type.getCname() + "*);\n");
           type.addConversionMethodCName(i,callBuilder.toString());
           if(!cEnv.isAbstract()){
               for(var method:i.getAbstractMethods()){
                   cEnv.appendToInitializer(cEnv.getImplOf(i) + "." +  method.getCname());
                   var impl =(Function)type.getField(method.getName(),cEnv);
                   cEnv.appendToInitializer("=&"  + impl.getCname()  + ";\n");
               }
           }
        }
        type.buildDeclaration(env);
        writer.write(cEnv.getInitializer("_cinit"));
        modEnv.appendToInitializer(cEnv.getInitializerCall());
		return TypeInfo.VOID;
	}
}