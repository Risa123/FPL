package risa.fpl.info;

import risa.fpl.env.ModuleEnv;

public final class InstanceInfo extends TypeInfo{
    private String attributesCode,implCode;
    private final ModuleEnv module;
    private boolean complete;
    public InstanceInfo(String name, String cname,ModuleEnv module) {
        super(name, cname);
        this.module = module;
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
    public void buildDeclaration() {
        complete = true;
        super.buildDeclaration();
    }
}