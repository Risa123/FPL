package risa.fpl.function.block;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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
    private ClassEnv getEnv(ModuleEnv modEnv,String id,TemplateStatus templateStatus,int tokenNum,int line)throws CompilerException{
        for(var classEnv:modEnv.getModuleBlock().getClassEnvList()){
            if(classEnv.getInstanceInfo().getName().equals(id)){
                return classEnv;
            }
        }
        modEnv.checkModifiers(line,tokenNum,Modifier.ABSTRACT,Modifier.FINAL);
        var env = new ClassEnv(modEnv,id,templateStatus,struct,line);
        modEnv.getModuleBlock().getClassEnvList().add(env);
        return env;
    }
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		if(!(env instanceof ModuleEnv modEnv)){
		    throw new CompilerException(line,tokenNum,"can only be used on module level");
        }
        var id = it.nextID();
		var idV = id.getValue();
        ClassEnv cEnv;
        LinkedHashMap<String,TypeInfo>templateArgs = null;
        if(it.checkTemplate()){
            cEnv = getEnv(modEnv,idV,TemplateStatus.TEMPLATE,tokenNum,line);
            templateArgs = IFunction.parseTemplateArguments(it,cEnv);
        }else{
            cEnv = getEnv(modEnv,idV,TemplateStatus.INSTANCE,tokenNum,line);
        }
		if(env.hasTypeInCurrentEnv(idV) && cEnv.getFirstLine() != line){
		    throw new CompilerException(id,"type " + idV + " is already declared");
        }
		InstanceInfo primaryParent = null;
		List block = null;
		var interfaces = new ArrayList<InterfaceInfo>();
        var type = cEnv.getInstanceInfo();
        while(it.hasNext()){
            var exp = it.next();
            if(exp instanceof List l){
                block = l;
                break;
            }else{
                var typeID = (Atom)exp;
                if(typeID.getType() != AtomType.ID){//do not remove error message is more precise than identifier or literal expected
                    throw new CompilerException(typeID,"identifier expected");
                }
                var parentType = env.getType(typeID);
                if(parentType.isPrimitive()){
                    throw new CompilerException(typeID,"primitive types cannot be inherited from");
                }
                if(!type.getParents().contains(parentType)){
                    if(parentType instanceof InterfaceInfo i){
                        interfaces.add(i);
                        type.addParent(parentType);
                    }else{
                        if(primaryParent != null){
                            throw new CompilerException(typeID,"there is already parent class");
                        }
                        if(parentType instanceof InstanceInfo t){
                            primaryParent = t;
                            if(t.isFinal()){
                                throw new CompilerException(typeID,"cannot inherit form final types");
                            }
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
                        cEnv.getInstanceInfo().setPrimaryParent(primaryParent);
                    }
                }
            }
        }
        if(block == null){
            throw new CompilerException(line,tokenNum,"block expected as last argument");
        }
		if(templateArgs != null){
            ((TemplateTypeInfo)type).setDataForGeneration(block,interfaces,templateArgs);
        }
        compileClassBlock(cEnv,modEnv,id,block,templateArgs == null?TemplateStatus.INSTANCE:TemplateStatus.TEMPLATE);
		return TypeInfo.VOID;
	}
	@SuppressWarnings("ConstantConditions")
    public void compileClassBlock(ClassEnv cEnv,ModuleEnv modEnv,Atom id,List block,TemplateStatus templateStatus)throws CompilerException{
        var type = cEnv.getInstanceInfo();
        var parentType = type.getPrimaryParent();
        var cID = IFunction.toCId(id.getValue());
        var attributes = new StringBuilder();
        ArrayList<ExpressionInfo>infos;
        if(cEnv.getBlock() == null){
            infos = createInfoList(block);
            cEnv.setBlock(infos);
        }else{
            infos = cEnv.getBlock();
        }
        var interfaces = new ArrayList<InterfaceInfo>();
        for(var p:type.getParents()){
            if(p instanceof InterfaceInfo i){
                interfaces.add(i);
            }
        }
        compile(new StringBuilder(),cEnv,infos);
        for(var name:cEnv.getVariableFieldDeclarationOrder()){
            var v = (Variable)type.getField(name,cEnv);
            if(v.getType() instanceof FunctionInfo f){
                attributes.append(f.getPointerVariableDeclaration(v.getCname()));
            }else{
                if(v.getType() instanceof PointerInfo p && p.getType() instanceof InstanceInfo){
                    attributes.append("struct ");
                }
                attributes.append(v.getType().getCname()).append(' ').append(v.getCname());
                if(v.getType() instanceof ArrayInfo i){
                    attributes.append('[').append(i.isLengthUnsignedLong()?Long.toUnsignedString(i.getLength()):Long.toString(i.getLength())).append(']');
                }
            }
            attributes.append(";\n");
        }
        if(cEnv.hasOnlyImplicitConstructor()){
            cEnv.appendFunctionCode(cEnv.getImplicitConstructor(id));
            var nameSpace = cEnv.getNameSpace();
            cEnv.appendFunctionCode(cID + " static" + nameSpace + "_new0(){\n");
            cEnv.appendFunctionCode(cID + " inst;\n" + INTERNAL_METHOD_PREFIX + nameSpace + "_init0(&inst);\nreturn inst;\n}\n");
            cEnv.appendFunctionCode(cID + "* static" + nameSpace + "_alloc0(){\n");
            cEnv.appendFunctionCode("void* malloc(" + NumberInfo.MEMORY.getCname() + ");\n");
            cEnv.appendFunctionCode(cID + " * p = malloc(sizeof(" + cID + "));\n");
            cEnv.appendFunctionCode(INTERNAL_METHOD_PREFIX + nameSpace + "_init0(p);\nreturn p;\n}\n");
        }
        type.setAttributesCode(attributes.toString());
        //parent type doesn't have implicit constructor
        if(parentType instanceof InstanceInfo i && !cEnv.isParentConstructorCalled()){
            for(var v:i.getConstructor().getVariants()){
               if(v.getArgs().length > 0){
                   throw new CompilerException(id,"constructor is required to call parent constructor");
               }
            }
        }
        if(cEnv.notAbstract()){
            for(var method:type.getMethodsOfType(FunctionType.ABSTRACT)){
                var name = method.getName();
                if(!(type.getField(name,cEnv) instanceof Function f) || f.getType() == FunctionType.ABSTRACT){
                    throw new CompilerException(id,"this class doesn't implement method " + name);
                }
            }
        }
        var internalCode = new StringBuilder();
        for(var i:interfaces){
            internalCode.append("static ").append(i.getImplName()).append(' ').append(cID).append(i.getCname()).append("_impl={");
            var first = true;
            for(var method:i.getMethodsOfType(FunctionType.ABSTRACT)){
                for(var v:method.getVariants()){
                    if(first){
                        first = false;
                    }else{
                        internalCode.append(',');
                    }
                    internalCode.append("(void*)").append(((Function)type.getField(method.getName(),cEnv)).getVariant(v.getArgs()).getCname());
                }
            }
            internalCode.append("};\n");
            internalCode.append(i.getCname()).append(' ');
            internalCode.append(type.getConversionMethod(i)).append('(').append(type.getCname()).append("* this){\n");
            internalCode.append(i.getCname());
            internalCode.append(" tmp={this,&");
            internalCode.append(cEnv.getImplOf(i));
            internalCode.append("};\nreturn tmp;\n}\n");
        }
        internalCode.append(type.getCname()).append("* ").append(type.getToPointerName()).append('(');
        internalCode.append(type.getCname()).append(" this,").append(type.getCname()).append("* p){\n*p = this;\nreturn p;\n}\n");
        var copyName = type.getCopyConstructorName();
        if(copyName == null && !cEnv.getImplicitCopyConstructorCode().isEmpty()){
            copyName = INTERNAL_METHOD_PREFIX + cEnv.getNameSpace() + "_copy";
            internalCode.append("void ").append(copyName).append('(').append(type.getCname()).append("* this,").append(type.getCname()).append("* o){\n");
            internalCode.append(cEnv.getImplicitCopyConstructorCode()).append(cEnv.getDefaultCopyConstructorCode()).append("}\n");
            type.setCopyConstructorName(copyName);
        }
        if(copyName != null){
            internalCode.append(type.getCname()).append(' ').append(copyName).append("AndReturn(").append(type.getCname()).append(" original){\n").append(type.getCname()).append(" instance;\n");
            internalCode.append(copyName).append("(&instance,&original);\nreturn instance;\n}\n");
        }
        cEnv.appendFunctionCode(internalCode.toString());
        if(templateStatus != TemplateStatus.TEMPLATE){
            modEnv.appendFunctionDeclaration(cEnv.getFunctionDeclarations());
            modEnv.appendFunctionCode(cEnv.getDestructor());
            if(type.getDestructorName() != null){
                cEnv.appendFunctionCode("void " + type.getInstanceFree() + '(' + type.getCname() + "* this){\nvoid free(void*);\n");
                cEnv.appendFunctionCode(type.getDestructorName() + "(this);\nfree(this);\n}\n");
            }
            modEnv.appendFunctionCode(cEnv.getFunctionCode());
            modEnv.appendFunctionCode(cEnv.getInitializer());
            modEnv.appendToInitializer(cEnv.getInitializerCall());
        }
    }
}