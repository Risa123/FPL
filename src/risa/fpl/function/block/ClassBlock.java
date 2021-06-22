package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public final class ClassBlock extends AThreePassBlock implements IFunction{
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum) throws IOException,CompilerException{
		if(!(env instanceof ModuleEnv modEnv)){
		    throw new CompilerException(line,charNum,"can only be used on module level");
        }
        var id = it.nextID();
		var idV = id.getValue();
		if(env.hasTypeInCurrentEnv(idV) && !(env.getType(id) instanceof InstanceInfo i && !i.isComplete())){
		    throw new CompilerException(id,"type " + idV +  " is already declared");
        }
		InstanceInfo parentType = null;
		List block = null;
		var interfaces = new ArrayList<InterfaceInfo>();
		LinkedHashMap<String,TypeInfo> templateArgs = null;
        ClassEnv cEnv;
		if(it.checkTemplate()){
            cEnv = new ClassEnv(modEnv,idV,TemplateStatus.TEMPLATE);
		    templateArgs = IFunction.parseTemplateArguments(it,cEnv);
        }else{
		    cEnv = new ClassEnv(modEnv,idV,TemplateStatus.INSTANCE);
        }
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
                    throw new CompilerException(typeID,"primitive types cannot be inherited from");
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
                        if(parentType instanceof TemplateTypeInfo){
                            if(!it.checkTemplate()){
                                throw new CompilerException(exp,"template arguments expected");
                            }
                            parentType = IFunction.generateTypeFor(t,typeID,it,env,false);
                        }
                    }else{
                        throw new CompilerException(typeID,"can only inherit from other classes");
                    }
                    cEnv.setPrimaryParent(parentType);
                }
            }
        }
        if(block == null){
            throw new CompilerException(line,charNum,"block expected as last argument");
        }
        BufferedWriter cWriter;
		if(templateArgs == null){
           cWriter = writer;
        }else{
		    cWriter = new BuilderWriter(writer);
            ((TemplateTypeInfo)cEnv.getInstanceType()).setDataForGeneration(block,interfaces,templateArgs);
        }
        compileClassBlock(cWriter,cEnv,modEnv,id,block,interfaces,templateArgs == null?TemplateStatus.INSTANCE:TemplateStatus.TEMPLATE);
		return TypeInfo.VOID;
	}
	public void compileClassBlock(BufferedWriter writer,ClassEnv cEnv,ModuleEnv modEnv,Atom id,List block,ArrayList<InterfaceInfo>interfaces,TemplateStatus templateStatus)throws CompilerException,IOException{
        var b = new BuilderWriter(writer);
        var type = cEnv.getInstanceType();
        var parentType = type.getPrimaryParent();
        b.write("typedef struct ");
        b.write(IFunction.toCId(id.getValue()));
        b.write("{\nvoid* object_data;\n");
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
        if(parentType instanceof InstanceInfo i && !cEnv.isParentConstructorCalled()){
            for(var v:i.getConstructor().getVariants()){
               if(v.args().length > 0){
                   throw new CompilerException(id.getLine(),id.getTokenNum(),"constructor is required to call parent constructor");
               }
            }
        }
        b.write('}');
        b.write(IFunction.toCId(id.getValue()));
        b.write(";\n");
        b.write(cEnv.getDataDeclaration());
        if(!cEnv.isAbstract()){
            for(var method:type.getMethodsOfType(FunctionType.ABSTRACT)){
                var name = method.getName();
                var impl = type.getField(name,cEnv);
                if(!(impl instanceof Function) || ((Function)impl).getType() == FunctionType.ABSTRACT){
                    throw new CompilerException(id.getLine(),id.getTokenNum(),"this class doesn't implement method " + name);
                }
            }
        }
        var internalCode = new BuilderWriter(writer);
        var constructor = type.getConstructor();
        if(constructor == null){
            constructor = new ClassVariable(type,cEnv.getClassType());
            constructor.addVariant(new TypeInfo[0],cEnv.getNameSpace());
            cEnv.addMethod(constructor,new TypeInfo[0],cEnv.getImplicitConstructor());
            type.setConstructor(constructor);
            cEnv.compileNewAndAlloc(internalCode,new TypeInfo[0],constructor);
        }
        type.appendToDeclaration(b.getCode());
        type.appendToDeclaration("extern " + cEnv.getDataDefinition());
        cEnv.appendDeclarations();
        var cID = IFunction.toCId(id.getValue());
        if(!modEnv.hasModifier(Modifier.ABSTRACT) && templateStatus != TemplateStatus.GENERATING){
            modEnv.addFunction(id.getValue(),constructor);
        }
        for(var i:interfaces){
            internalCode.write("static " + i.getImplName() + " " + cID + i.getCname() + "_impl;\n");
            internalCode.write(i.getCname() + ' ');
            var callBuilder = new StringBuilder(INTERNAL_METHOD_PREFIX);
            callBuilder.append(cEnv.getNameSpace());
            callBuilder.append("_as");
            callBuilder.append(i.getCname());
            var asCName = callBuilder.toString();
            internalCode.write(asCName);
            internalCode.write('(');
            internalCode.write(type.getCname());
            internalCode.write("* this){\n");
            internalCode.write(i.getCname());
            internalCode.write(" tmp;\ntmp.instance=this;\ntmp.impl=&");
            internalCode.write(cEnv.getImplOf(i));
            internalCode.write(";\nreturn tmp;\n}\n");
            type.appendToDeclaration(i.getCname() + " " + callBuilder + "(" + type.getCname() + "*);\n");
            type.addConversionMethodCName(i,callBuilder.toString());
            var parent = type.getPrimaryParent();
            if(!cEnv.isAbstract() && parent != null){
                for(var method:parent.getMethodsOfType(FunctionType.VIRTUAL)){
                    for(var v:method.getVariants()){
                        cEnv.appendToInitializer(cEnv.getDataName() + "." + v.implName() + "=&" + v.cname() + ";\n");
                    }
                }
                for(var method:i.getMethodsOfType(FunctionType.ABSTRACT)){
                  for(var v:method.getVariants()){
                      var impl =(Function)type.getField(method.getName(),cEnv);
                      for(var v1:impl.getVariants()){
                          cEnv.appendToInitializer(cEnv.getImplOf(i) + "." +  v.cname());
                          cEnv.appendToInitializer("=&"  + v1.cname() + ";\n");
                      }
                  }
                }
            }
        }
        internalCode.write("static " + type.getCname() + " container" + type.getCname() + ";\n");
        internalCode.write(type.getCname() + "* " + INTERNAL_METHOD_PREFIX + cEnv.getNameSpace() + "_toPointer(");
        internalCode.write(type.getCname() + " this){\ncontainer" + type.getCname() + " = this;\n");
        internalCode.write("return &container" + type.getCname() + ";\n}\n");
        cEnv.appendFunctionCode(internalCode.getCode());
        if(templateStatus != TemplateStatus.GENERATING){
            writer.write(cEnv.getDataDefinition());
        }
        if(templateStatus != TemplateStatus.TEMPLATE){
            modEnv.appendFunctionDeclarations(cEnv.getFunctionDeclarations());
            modEnv.appendFunctionCode(cEnv.getDestructor());
            if(type.getDestructorName() != null){
                cEnv.appendFunctionCode("void " + type.getInstanceFree() + "(" + type.getCname() + "* this){\n");
                cEnv.appendFunctionCode(type.getDestructorName() + "(this);\nfree(this);\n}\n");
            }
            modEnv.appendFunctionCode(cEnv.getFunctionCode());
            modEnv.appendFunctionCode(cEnv.getInitializer());
            modEnv.appendToInitializer(cEnv.getInitializerCall());
            type.buildDeclaration();
        }
    }
}