package risa.fpl.info;

public final class ArrayInfo extends PointerInfo{
    private final long length;
    private final boolean lengthUnsignedLong;
    public ArrayInfo(TypeInfo type,long length,boolean lengthUnsignedLong){
        super(type);
        this.length = length;
        this.lengthUnsignedLong = lengthUnsignedLong;
    }
    public long getLength(){
        return length;
    }
    public boolean isLengthUnsignedLong(){
        return lengthUnsignedLong;
    }
    public String getCname(){
        return getType().getCname();
    }
}