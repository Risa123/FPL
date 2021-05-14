package risa.fpl.info;

public final class FunctionPointer extends TypeInfo{
    public FunctionPointer(String name,String cname){
        super(name + "*",cname,true);
    }
}