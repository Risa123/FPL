package risa.fpl.info;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.GetObjectInfo;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.function.statement.ClassVariable;

public class InstanceInfo extends TypeInfo{
    private String attributesCode,implCode,destructorName,instanceFree,copyConstructorName;
    private final ModuleEnv module;
    private boolean complete;
    private ClassVariable constructor;
    private final String toPointerName;
    public InstanceInfo(String name,ModuleEnv module,String nameSpace){
        super(name,IFunction.toCId(name));
        this.module = module;
        addField("getObjectSize",new GetObjectInfo(NumberInfo.MEMORY,"size",this));
        instanceFree = "free";
        toPointerName = IFunction.INTERNAL_METHOD_PREFIX + nameSpace + "_toPointer";
    }
    public final String getClassDataType(){
        return getCname() + "_data_type*";
    }
    public final String getAttributesCode(){
        return attributesCode;
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
        for(var t:constructor.getRequiredTypes()){
            addRequiredType(t);
        }
        super.buildDeclaration();
    }
    @Override
    public final void setClassInfo(ClassInfo info){
        super.setClassInfo(info);
        getClassInfo().addField("getInstanceSize",new ValueExp(NumberInfo.MEMORY,"sizeof(" + getCname() + ")"));
    }
    @Override
    protected final boolean identical(TypeInfo type){
        var nameResult = super.identical(type);
        if(type instanceof InstanceInfo i && nameResult){
            return module.getNameSpace().equals(i.module.getNameSpace());
        }
        return nameResult;
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
    public final ClassVariable getConstructor(){
        return constructor;
    }
    public final void setConstructor(ClassVariable constructor){
        this.constructor = constructor;
        for(var v:constructor.getVariants()){
            for(var arg:v.args()){
                addRequiredType(arg);
            }
        }
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
}