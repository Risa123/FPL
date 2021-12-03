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
import risa.fpl.function.exp.Variable;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

public final class ClassBlock extends AThreePassBlock implements IFunction{
    private final boolean struct;
    public ClassBlock(boolean struct){
       this.struct = struct;
    }
    private ClassEnv getEnv(ModuleEnv modEnv,String id,TemplateStatus templateStatus){
        for(var classEnv:modEnv.getModuleBlock().getClassEnvList()){
            if(classEnv.getInstanceType().getName().equals(id)){
                return classEnv;
            }
        }
        var env = new ClassEnv(modEnv,id,templateStatus,struct);
        modEnv.getModuleBlock().getClassEnvList().add(env);
        return env;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum) throws IOException,CompilerException{
		if(!(env instanceof ModuleEnv modEnv)){
		    throw new CompilerException(line,tokenNum,"can only be used on module level");
        }
        var id = it.nextID();
		var idV = id.getValue();
		if(env.hasTypeInCurrentEnv(idV) && !(env.getType(id) instanceof InstanceInfo i && !i.isComplete())){
		    throw new CompilerException(id,"type " + idV +  " is already declared");
        }
		InstanceInfo primaryParent = null;
		List block = null;
		var interfaces = new ArrayList<InterfaceInfo>();
		LinkedHashMap<String,TypeInfo>templateArgs = null;
        ClassEnv cEnv;
		if(it.checkTemplate()){
            cEnv = getEnv(modEnv,idV,TemplateStatus.TEMPLATE);
		    templateArgs = IFunction.parseTemplateArguments(it,cEnv);
        }else{
		    cEnv = getEnv(modEnv,idV,TemplateStatus.INSTANCE);
        }
        var type = cEnv.getInstanceType();
        while(it.hasNext()){
            var exp = it.next();
            if(exp instanceof List l){
                block = l;
                break;
            }else{
                var typeID = (Atom)exp;
                if(typeID.getType() != AtomType.ID){
                    throw new CompilerException(typeID,"identifier expected");
                }
                var parentType = env.getType(typeID);
                if(parentType.isPrimitive()){
                    throw new CompilerException(typeID,"primitive types cannot be inherited from");
                }
                type.addParent(parentType);
                if(parentType instanceof InterfaceInfo i){
                   interfaces.add(i);
                }else{
                    if(primaryParent != null){
                        throw new CompilerException(typeID,"there is already parent class");
                    }
                    if(parentType instanceof InstanceInfo t){
                        primaryParent = t;
                        if(primaryParent instanceof TemplateTypeInfo){
                            if(!it.checkTemplate()){
                                throw new CompilerException(exp,"template arguments expected");
                            }
                            primaryParent = IFunction.generateTypeFor(t,typeID,it,env,false);
                        }
                    }else{
                        throw new CompilerException(typeID,"can only inherit from other classes");
                    }
                    if(struct){
                        throw new CompilerException(line,tokenNum,"struct cannot inherit");
                    }
                    cEnv.setPrimaryParent(primaryParent);
                }
            }
        }
        if(block == null){
            throw new CompilerException(line,tokenNum,"block expected as last argument");
        }
        BufferedWriter cWriter;
		if(templateArgs == null){
           cWriter = writer;
        }else{
		    cWriter = new BuilderWriter();
            ((TemplateTypeInfo)type).setDataForGeneration(block,interfaces,templateArgs);
        }
        compileClassBlock(cWriter,cEnv,modEnv,id,block,interfaces,templateArgs == null?TemplateStatus.INSTANCE:TemplateStatus.TEMPLATE);
		return TypeInfo.VOID;
	}
	public void compileClassBlock(BufferedWriter writer,ClassEnv cEnv,ModuleEnv modEnv,Atom id,List block,ArrayList<InterfaceInfo>interfaces,TemplateStatus templateStatus)throws CompilerException,IOException{
        var type = cEnv.getInstanceType();
        var parentType = type.getPrimaryParent();
        var cID = IFunction.toCId(id.getValue());
        var attributes = new BuilderWriter();
        ArrayList<ExpressionInfo>infos;
        if(cEnv.getBlock() == null){
            infos = createInfoList(block);
            cEnv.setBlock(infos);
        }else{
            infos = cEnv.getBlock();
        }
        try{
            compile(new BuilderWriter(),cEnv,infos);
        }catch(CompilerException ex){
            ex.setSourceFile("");
            throw ex;
        }
        for(var field:type.getFields().values()){
            if(field instanceof Variable v && !v.getCname().equals("objectData")){
               if(v.getType() instanceof FunctionInfo f){
                   attributes.write(f.getPointerVariableDeclaration(v.getCname()) + ";\n");
               }else{
                   if(v.getType() instanceof PointerInfo p && p.getType() instanceof InstanceInfo){
                       attributes.write("struct ");
                   }
                   attributes.write(v.getType().getCname() + " " + v.getCname() + ";\n");
               }
            }
        }
        type.setAttributesCode(attributes.getCode());
        //parent type doesn't have implicit constructor
        if(parentType instanceof InstanceInfo i && !cEnv.isParentConstructorCalled()){
            for(var v:i.getConstructor().getVariants()){
               if(v.args().length > 0){
                   throw new CompilerException(id,"constructor is required to call parent constructor");
               }
            }
        }
        if(!cEnv.isAbstract()){
            for(var method:type.getMethodsOfType(FunctionType.ABSTRACT)){
                var name = method.getName();
                var impl = type.getField(name,cEnv);
                if(!(impl instanceof Function f) || f.getType() == FunctionType.ABSTRACT){
                    throw new CompilerException(id,"this class doesn't implement method " + name);
                }
            }
        }
        var internalCode = new BuilderWriter();
        var constructor = type.getConstructor();
        if(constructor.getVariants().size() == 0){
            constructor.addVariant(new TypeInfo[0],cEnv.getNameSpace());
            cEnv.addMethod(constructor,cEnv.getImplicitConstructor());
            type.setConstructor(constructor);
            cEnv.getSuperEnv().addFunction(type.getName(),constructor);
            cEnv.compileNewAndAlloc(internalCode,new TypeInfo[0],constructor);
        }
        if(!modEnv.hasModifier(Modifier.ABSTRACT) && templateStatus != TemplateStatus.GENERATING){
            modEnv.addFunction(id.getValue(),constructor);
        }
        for(var i:interfaces){
            internalCode.write("static " + i.getImplName() + " " + cID + i.getCname() + "_impl;\n");
            internalCode.write(i.getCname() + ' ');
            internalCode.write(type.getConversionMethod(i) + '(' + type.getCname() + "* this){\n");
            internalCode.write(i.getCname());
            internalCode.write(" tmp;\ntmp.instance=this;\ntmp.impl=&");
            internalCode.write(cEnv.getImplOf(i));
            internalCode.write(";\nreturn tmp;\n}\n");
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
        internalCode.write(type.getCname() + "* " + type.getToPointerName() + "(");
        internalCode.write(type.getCname() + " this," + type.getCname() + "* p){\n*p = this;\n");
        internalCode.write("return p;\n}\n");
        var copyName = type.getCopyConstructorName();
        if(copyName == null && !cEnv.getImplicitCopyConstructorCode().isEmpty()){
            copyName = INTERNAL_METHOD_PREFIX + cEnv.getNameSpace() + "_copy";
            internalCode.write(copyName + "(" + type.getCname() + "* this," + type.getCname() + "* o){\n");
            internalCode.write(cEnv.getImplicitCopyConstructorCode() + cEnv.getDefaultCopyConstructorCode() + "}\n");
            type.setCopyConstructorName(copyName);
        }
        if(copyName != null){
            internalCode.write(type.getCname() + " " + copyName + "AndReturn(" + type.getCname() + " original){\n" + type.getCname() + " instance;\n");
            internalCode.write(copyName + "(&instance,&original);\n");
            internalCode.write("return instance;\n}\n");
        }
        cEnv.appendFunctionCode(internalCode.getCode());
        if(templateStatus != TemplateStatus.TEMPLATE){
            modEnv.appendFunctionDeclaration(cEnv.getFunctionDeclarations());
            modEnv.appendFunctionCode(cEnv.getDestructor());
            if(type.getDestructorName() != null){
                cEnv.appendFunctionCode("void " + type.getInstanceFree() + "(" + type.getCname() + "* this){\n");
                cEnv.appendFunctionCode(type.getDestructorName() + "(this);\nfree(this);\n}\n");
            }
            modEnv.appendFunctionCode(cEnv.getFunctionCode());
            modEnv.appendFunctionCode(cEnv.getInitializer());
            modEnv.appendToInitializer(cEnv.getInitializerCall());
        }
    }
}