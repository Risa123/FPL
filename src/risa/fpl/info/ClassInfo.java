package risa.fpl.info;

import risa.fpl.function.exp.Alloc;
import risa.fpl.function.exp.PointerSize;
import risa.fpl.function.exp.ValueExp;

public final class ClassInfo extends TypeInfo{
    public static final ClassInfo CHAR = new ClassInfo("char");
    public static final ClassInfo BOOL = new ClassInfo("bool");
    public static final ClassInfo UBYTE = new ClassInfo("ubyte");
    public static final ClassInfo SBYTE = new ClassInfo("sbyte");
    public static final ClassInfo BYTE =  new ClassInfo("byte");
    public static final ClassInfo SHORT = new ClassInfo("short");
    public static final ClassInfo USHORT = new ClassInfo("ushort");
    public static final ClassInfo SSHORT = new ClassInfo("sshort");
    public static final ClassInfo INT = new ClassInfo("int");
    public static final ClassInfo UINT = new ClassInfo("uint");
    public static final ClassInfo SINT = new ClassInfo("sint");
    public static final ClassInfo LONG = new ClassInfo("long");
    public static final ClassInfo ULONG = new ClassInfo("ulong");
    public static final ClassInfo SLONG = new ClassInfo("slong");
    public static final ClassInfo DOUBLE = new ClassInfo("double");
    public static final ClassInfo FLOAT = new ClassInfo("float");
    public static final ClassInfo OBJECT = new ClassInfo("object");
    public static final ClassInfo MEMORY = new ClassInfo("memory");
    public static final ClassInfo POINTER = new ClassInfo("pointer");
    public static final ClassInfo FUNCTION = new ClassInfo("function");
    private TypeInfo instanceInfo;
    static{
        CHAR.init(TypeInfo.CHAR);
        BOOL.init(TypeInfo.BOOL);
        UBYTE.init(NumberInfo.UBYTE);
        SBYTE.init(NumberInfo.SBYTE);
        BYTE.init(NumberInfo.BYTE);
        SHORT.init(NumberInfo.SHORT);
        SSHORT.init(NumberInfo.SSHORT);
        USHORT.init(NumberInfo.USHORT);
        UINT.init(NumberInfo.UINT);
        SINT.init(NumberInfo.SINT);
        INT.init(NumberInfo.INT);
        ULONG.init(NumberInfo.ULONG);
        SLONG.init(NumberInfo.SLONG);
        LONG.init(NumberInfo.LONG);
        FLOAT.init(NumberInfo.FLOAT);
        DOUBLE.init(NumberInfo.DOUBLE);
        OBJECT.init(TypeInfo.OBJECT);
        POINTER.addField("getInstanceSize",PointerSize.INSTANCE);
        FUNCTION.addField("getInstanceSize",PointerSize.INSTANCE);
    }
    public ClassInfo(String name){
        super(name + " class","");
    }
    private void init(TypeInfo type){
        type.setClassInfo(this);
        addField("getInstanceSize",new ValueExp(NumberInfo.MEMORY,"sizeof(" + instanceInfo.getCname() + ')'));
        addField("alloc",new Alloc(instanceInfo,false));
        addField("alloc[]",new Alloc(instanceInfo,true));
    }
    public TypeInfo getInstanceInfo(){
        return instanceInfo;
    }
    public void setInstanceInfo(TypeInfo type){
        instanceInfo = type;
    }
}