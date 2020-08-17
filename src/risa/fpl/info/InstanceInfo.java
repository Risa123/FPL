package risa.fpl.info;

public final class InstanceInfo extends TypeInfo{
    public InstanceInfo(String name, String cname) {
        super(name, cname);
    }
    public String getClassDataType(){
        return getCname() + "_data_type*";
    }
}