package risa.fpl.info;

import risa.fpl.env.ClassEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.*;
import risa.fpl.function.statement.InstanceVar;

public class InstanceInfo extends NonTrivialTypeInfo{
    private String attributesCode,implCode,destructorName,instanceFree = "free",copyConstructorName;
    private boolean complete;
    private InstanceVar constructor;
    private final String toPointerName;
    private String methodDeclarations = "";
    private ClassEnv cEnv;
    public InstanceInfo(String name,ModuleEnv module,String nameSpace){
        super(module,name,IFunction.toCId(name));
        addField("getObjectSize",new GetObjectInfo(NumberInfo.MEMORY,"size",this));
        addField("getClass",new Variable(new PointerInfo(TypeInfo.VOID),"objectData",false,"getClass",true,this,AccessModifier.PUBLIC));
        addField("cast",new Cast(this));
        toPointerName = IFunction.INTERNAL_METHOD_PREFIX + nameSpace + "_toPointer";
    }
    public final String getClassDataType(){
        return getCname() + "_data_type*";
    }
    public final void setAttributesCode(String attributesCode){
        this.attributesCode = attributesCode;
    }
    public final String getImplCode(){
        return implCode;
    }
    public final void setImplCode(String implCode){
        this.implCode = implCode;
    }
    public final ModuleEnv getModule(){
        return module;
    }
    public final boolean isComplete(){
        return complete;
    }
    @Override
    public final void buildDeclaration(){
        complete = true;
        appendToDeclaration("typedef struct " + getCname() + "{\n");
        if(!cEnv.isStruct()){
            appendToDeclaration("void* objectData;\n");
        }
        if(getPrimaryParent() instanceof InstanceInfo i){
            appendToDeclaration(i.attributesCode);
        }
        appendToDeclaration(attributesCode);
        appendToDeclaration("}" + getCname() + ";\n");
        var b = new StringBuilder();
        setImplCode(cEnv.getImplCode());
        var primaryParent = (InstanceInfo)getPrimaryParent();
        appendToDeclaration("typedef struct " + cEnv.getDataType());
        appendToDeclaration(cEnv.getDataType() + "{\nunsigned long size;\n");
        if(primaryParent != null){
            appendToDeclaration(primaryParent.getImplCode());
        }
        appendToDeclaration(cEnv.getImplCode() + '}' + cEnv.getDataType() + ";\n");
        appendToDeclaration("extern " + cEnv.getDataDefinition());
        addFunctionRequiredTypes(constructor);
        appendToDeclaration(getCname() + "* " + getToPointerName() + "(" + getCname() + " this," + getCname() + "* p);\n");
        for(var field:getClassInfo().getFields().values()){
            if(field instanceof Function f){
                cEnv.appendFunctionDeclaration(f);
            }
        }
        for(var field:getFields().values()){
            if(field instanceof Function f){
                if(f.getAccessModifier() != AccessModifier.PRIVATE){
                    cEnv.appendFunctionDeclaration(f);
                }
            }
        }
        if(!(this instanceof TemplateTypeInfo)){
            setMethodDeclarations(cEnv.getFunctionDeclarations());
        }
        if(copyConstructorName != null){
            appendToDeclaration(getCname() + " " + copyConstructorName + "AndReturn(" + getCname() + " original);\n");
        }
        for(var p:parents){
            if(p instanceof InterfaceInfo i){
                appendToDeclaration(i.getCname() + " " + getConversionMethod(i) + "(" + getCname() + "*);\n");
            }
        }
        super.buildDeclaration();
    }
    @Override
    public final void setClassInfo(ClassInfo info){
        super.setClassInfo(info);
        getClassInfo().addField("getInstanceSize",new ValueExp(NumberInfo.MEMORY,"sizeof(" + getCname() + ")"));
    }
    public final String getDestructorName(){
        return destructorName;
    }
    public final String getInstanceFree(){
        return instanceFree;
    }
    public void setDestructorName(String prefix){
        destructorName = prefix + "_destructor";
        instanceFree = prefix + "_free";
    }
    public final InstanceVar getConstructor(){
        return constructor;
    }
    public final void setConstructor(InstanceVar constructor){
        this.constructor = constructor;
        addFunctionRequiredTypes(constructor);
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
        return IFunction.INTERNAL_METHOD_PREFIX + module.getNameSpace() + getCname() + "_as" + i.getCname();
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
}