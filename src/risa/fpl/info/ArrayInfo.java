package risa.fpl.info;

public final class ArrayInfo extends PointerInfo{
    private final long length;
    public ArrayInfo(TypeInfo type,long length){
        super(type);
        this.length = length;
    }
    public long getLength(){
        return length;
    }
    public String getCname(){
        return getType().getCname();
    }
}