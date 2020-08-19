package risa.fpl.info;

public final class InstanceInfo extends TypeInfo{
    private String attributesCode;
    public InstanceInfo(String name, String cname) {
        super(name, cname);
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
}