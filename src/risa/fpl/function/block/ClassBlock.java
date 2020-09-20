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
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.PointerInfo;
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
		if(env.hasTypeInCurrentEnv(idV) && !(env.getType(id) instanceof InstanceInfo i && !i.isComplete())){
		    throw new CompilerException(id,"this type is already declared");
        }
        String cID = IFunction.toCId(id.getValue());
        var cEnv = new ClassEnv(modEnv,cID,idV);
		InstanceInfo parentType = null;
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
                    if(type instanceof InstanceInfo t){
                        parentType = t;
                    }else{
                        throw new CompilerException(typeID,"can only inherit from other classes");
                    }
                    cEnv.getInstanceType().setPrimaryParent(parentType);
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
	    b.write("void* object_data;\n");
	    if(parentType instanceof InstanceInfo i){
            b.write(i.getAttributesCode());
        }
	    var attributes = new BuilderWriter(b);
        try{
            compile(attributes,cEnv,block);
        }catch(CompilerException ex){
            ex.setSourceFile("");
            throw ex;
        }
        cEnv.getInstanceType().setAttributesCode(attributes.getCode());
        b.write(attributes.getCode());
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
        writer.write(b.getCode() + cEnv.getDataDefinition());
        var constructor = type.getConstructor();
        if(constructor == null){
            constructor = new ClassVariable(type,cEnv.getClassType(),new TypeInfo[]{},cEnv.getNameSpace(this),env);
            cEnv.addMethod(constructor,cEnv.getImplicitConstructor());
            type.setConstructor(constructor);
        }
        writer.write(cEnv.getMethodCode());
        var allocName = "static" + cEnv.getNameSpace() + "_alloc";
        type.appendToDeclaration(b.getCode());
        cEnv.appendDeclarations();
        if(!env.hasModifier(Modifier.ABSTRACT)){
            writer.write(cID + "* " + allocName + "(");
            var args = constructor.getArguments();
            var compiledArgs = constructorArguments(args);
            writer.write(compiledArgs);
            writer.write("){\n");
            writer.write("void* malloc(unsigned long);\n");
            writer.write(type.getCname());
            writer.write("* p=malloc(sizeof(");
            writer.write(type.getCname());
            writer.write("));\n");
            writer.write(constructorCall(constructor,"p"));
            writer.write("\nreturn p;\n}\n");
            var allocMethod = Function.newStatic("alloc",new PointerInfo(type),args,cEnv);
            var classType = cEnv.getClassType();
            classType.addField("alloc",allocMethod);
            writer.write(allocMethod.getDeclaration());
            type.appendToDeclaration(allocMethod.getDeclaration());
            var newName = "static" + cEnv.getNameSpace() + "_new";
            writer.write(type.getCname() + " " + newName + "(" + compiledArgs + "){\n" + type.getCname() + " inst;\n");
            writer.write(constructorCall(constructor,"&inst"));
            writer.write("return inst;\n}\n");
            var newMethod = Function.newStatic("new",type,args,cEnv);
            classType.addField("new",newMethod);
            type.appendToDeclaration(newMethod.getDeclaration());
        }
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
        type.buildDeclaration();
        writer.write(cEnv.getInitializer("_cinit"));
        modEnv.appendToInitializer(cEnv.getInitializerCall());
		return TypeInfo.VOID;
	}
	private String constructorArguments(TypeInfo[]args){
        var first = true;
        var b = new StringBuilder();
        for(int i = 0; i < args.length;++i){
            if(first){
                first = false;
            }else{
                b.append(',');
            }
            b.append(args[i].getCname()).append(" a").append(i);
        }
        return b.toString();
    }
    private String constructorCall(Function constructor,String self){
	    var b = new StringBuilder(constructor.getCname());
        b.append("(");
        b.append(self);
        for(int i = 0; i < constructor.getArguments().length;++i){
            b.append(",a");
            b.append(i);
        }
        b.append(");\n");
        return b.toString();
    }
}