package risa.fpl.info;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.GetObjectInfo;
import risa.fpl.function.exp.ValueExp;

public class InstanceInfo extends TypeInfo{
    private String attributesCode,implCode;
    private final ModuleEnv module;
    private boolean complete;
    public InstanceInfo(String name,ModuleEnv module){
        super(name,IFunction.toCId(name));
        this.module = module;
        addField("getObjectSize",new GetObjectInfo(NumberInfo.MEMORY,"size",this));
    }
    public String getClassDataType(){
        return getCname() + "_data_type*";
    }
    public String getAttributesCode(){
        return attributesCode;
    }
    public void setAttributesCode(String attributesCode){
        this.attributesCode = attributesCode;
    }
    public String getImplCode(){
        return implCode;
    }
    public void setImplCode(String implCode){
        this.implCode = implCode;
    }
    public ModuleEnv getModule(){
        return module;
    }
    public boolean isComplete(){
        return complete;
    }
    @Override
    public void buildDeclaration(){
        complete = true;
        super.buildDeclaration();
    }
    @Override
    public void setClassInfo(ClassInfo info){
        super.setClassInfo(info);
        getClassInfo().addField("getInstanceSize",new ValueExp(NumberInfo.MEMORY,"sizeof(" + getCname() + ")"));
    }
}