package risa.fpl.info;

import risa.fpl.env.ClassEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.*;
import risa.fpl.function.statement.InstanceVar;

import java.util.HashMap;

public class InstanceInfo extends NonTrivialTypeInfo {
    private String attributesCode,destructorName,instanceFree = "free",copyConstructorName,methodDeclarations = "";
    private final InstanceVar constructor = new InstanceVar(this);
    private final String toPointerName,dataName;
    private ClassEnv cEnv;
    private boolean writeTemplateFunctionVariants = true;
    private final boolean isFinal;
    public InstanceInfo(String name,ModuleEnv module,String nameSpace,boolean isFinal){
        super(module,name,IFunction.toCId(name));
        this.isFinal = isFinal;
        toPointerName = IFunction.INTERNAL_PREFIX + nameSpace + "_toPointer";
        dataName = nameSpace + "_data";
        addField("getObjectSize",new GetObjectInfo(NumberInfo.MEMORY,"size",this));
        addField("getClass",new Variable(new PointerInfo(TypeInfo.VOID),"objectData",false,"getClass",true,this,AccessModifier.PUBLIC));
        addField("cast",new Cast(this));
        getClassInfo().addField("alloc",new Function("alloc",new PointerInfo(this),null,AccessModifier.PUBLIC));
        getClassInfo().addField("new",new Function("new",this,null,AccessModifier.PUBLIC));
    }
    public final String getClassDataType(){
        return getCname() + "_data_type";
    }
    public final void setAttributesCode(String attributesCode){
        this.attributesCode = attributesCode;
    }
    public final ModuleEnv getModule(){
        return module;
    }
    @Override
    public final void buildDeclaration(){
        if(!(this instanceof TemplateTypeInfo)){
            appendToDeclaration("typedef struct " + getCname() + "{\n");
            if(cEnv.notStruct()){
                appendToDeclaration("void* objectData;\n");
            }
            if(getPrimaryParent() instanceof InstanceInfo i){
                appendToDeclaration(i.attributesCode);
            }
            appendToDeclaration(attributesCode);
            appendToDeclaration('}' + getCname() + ";\n");
            var b = new StringBuilder();
            var methods = new HashMap<String,Function>();
            for(var entry:getMethodVariantsOfType(FunctionType.VIRTUAL).entrySet()){
                methods.put(entry.getValue().getName(),entry.getValue());
            }
            for(var entry:getMethodVariantsOfType(FunctionType.ABSTRACT).entrySet()){
                methods.put(entry.getValue().getName(),entry.getValue());
            }
            for(var method:methods.values()){
                for(var v:method.getVariants()){
                    b.append(new FunctionInfo(method).getPointerVariableDeclaration(v.getImplName())).append(";\n");
                }
            }
            appendToDeclaration("typedef struct " + getClassDataType());
            appendToDeclaration("{\nunsigned long size;\n" + b);
            appendToDeclaration('}' + getClassDataType() + ";\n");
            appendToDeclaration("extern " + getClassDataType() + ' ' + dataName + ";\n");
            addFunctionRequiredTypes(constructor);
            appendToDeclaration(getCname() + "* " + getToPointerName() + '(' + getCname() + " this," + getCname() + "* p);\n");
            for(var field:getClassInfo().getFields().values()){
                if(field instanceof Function f){
                    cEnv.appendFunctionDeclaration(f);
                }
            }
            for(var field:getFields().values()){
                if(field instanceof Function f && f.getAccessModifier() != AccessModifier.PRIVATE){
                    cEnv.appendFunctionDeclaration(f);
                }
            }
            if(cEnv.hasOnlyImplicitConstructor()){
                cEnv.appendFunctionDeclaration("void " + IFunction.INTERNAL_PREFIX + cEnv.getNameSpace() + "_init0(" + getCname() + "*);\n");
            }
            cEnv.appendFunctionDeclaration(constructor);
            if(copyConstructorName != null){
                appendToDeclaration("void " + copyConstructorName + '(' + getCname() + "*," + getCname() + "*);\n");
                appendToDeclaration(getCname() + ' ' + copyConstructorName + "AndReturn(" + getCname() + " original);\n");
            }
            if(destructorName != null){
                appendToDeclaration("void " + destructorName + '(' + getCname() + "*);\n");
            }
            for(var p:parents){
                if(p instanceof InterfaceInfo i){
                    appendToDeclaration(i.getCname() + ' ' + getConversionMethod(i) + '(' + getCname() + "*);\n");
                }
            }
            appendToDeclaration("void " + module.getNameSpace() + "_freeLEFT_SQUARE_BRACKETRIGHT_SQUARE_BRACKET" + getCname() + "ASTERISK");
            appendToDeclaration(NumberInfo.MEMORY.getCname().replace(' ','_') +"0("+ getCname() + "*," + NumberInfo.MEMORY.getCname() + ");\n");
            setMethodDeclarations(cEnv.getFunctionDeclarations());
        }
    }
    @Override
    public final void setClassInfo(ClassInfo info){
        super.setClassInfo(info);
        getClassInfo().addField("getInstanceSize",new ValueExp(NumberInfo.MEMORY,"sizeof(" + getCname() + ')'));
    }
    public final String getDestructorName(){
        return destructorName;
    }
    public final String getInstanceFree(){
        return instanceFree;
    }
    public final void setDestructorName(String prefix){
        destructorName = prefix + "_destructor";
        instanceFree = prefix + "_free";
    }
    public final InstanceVar getConstructor(){
        return constructor;
    }
    public final String getCopyConstructorName(){
        return copyConstructorName;
    }
    public final void setCopyConstructorName(String name){
        copyConstructorName = name;
    }
    public final String getToPointerName(){
        return toPointerName;
    }
    public final void setMethodDeclarations(String code){
        methodDeclarations = code;
    }
    public final String getMethodDeclarations(){
        return methodDeclarations;
    }
    public final String getConversionMethod(InterfaceInfo i){
        return IFunction.INTERNAL_PREFIX + module.getNameSpace() + getCname() + "_as" + i.getCname();
    }
    public final boolean isException(){
        if(module.getNameSpace().equals("_std_lang") && getName().equals("Exception")){
            return true;
        }else if(getPrimaryParent() instanceof InstanceInfo i){
            return i.isException();
        }
        return false;
    }
    public final void setClassEnv(ClassEnv cEnv){
        this.cEnv = cEnv;
    }
    public final boolean canWriteTemplateFunctionVariants(){
        return writeTemplateFunctionVariants;
    }
    public final void disableWriteTemplateFunctionVariants(){
        writeTemplateFunctionVariants = false;
    }
    public final String getDataName(){
        return dataName;
    }
    public final boolean isFinal(){
        return isFinal;
    }
}