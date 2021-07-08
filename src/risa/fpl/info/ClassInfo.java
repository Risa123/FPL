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
    private TypeInfo instanceType;
    private final String dataName;
    static{
        TypeInfo.CHAR.setClassInfo(CHAR);
        TypeInfo.BOOL.setClassInfo(BOOL);
        TypeInfo.OBJECT.setClassInfo(OBJECT);
        NumberInfo.UBYTE.setClassInfo(UBYTE);
        NumberInfo.SBYTE.setClassInfo(SBYTE);
        NumberInfo.BYTE.setClassInfo(BYTE);
        NumberInfo.SHORT.setClassInfo(SHORT);
        NumberInfo.SSHORT.setClassInfo(SSHORT);
        NumberInfo.USHORT.setClassInfo(USHORT);
        NumberInfo.INT.setClassInfo(INT);
        NumberInfo.SINT.setClassInfo(SINT);
        NumberInfo.UINT.setClassInfo(UINT);
        NumberInfo.LONG.setClassInfo(LONG);
        NumberInfo.ULONG.setClassInfo(ULONG);
        NumberInfo.SLONG.setClassInfo(SLONG);
        NumberInfo.FLOAT.setClassInfo(FLOAT);
        NumberInfo.DOUBLE.setClassInfo(DOUBLE);
        NumberInfo.MEMORY.setClassInfo(MEMORY);
        CHAR.addMethods(NumberInfo.CHAR);
        BOOL.addMethods(TypeInfo.BOOL);
        UBYTE.addMethods(NumberInfo.UBYTE);
        SBYTE.addMethods(NumberInfo.SBYTE);
        BYTE.addMethods(NumberInfo.BYTE);
        SHORT.addMethods(NumberInfo.SHORT);
        SSHORT.addMethods(NumberInfo.SSHORT);
        USHORT.addMethods(NumberInfo.USHORT);
        UINT.addMethods(NumberInfo.UINT);
        SINT.addMethods(NumberInfo.SINT);
        INT.addMethods(NumberInfo.INT);
        ULONG.addMethods(NumberInfo.ULONG);
        SLONG.addMethods(NumberInfo.SLONG);
        LONG.addMethods(NumberInfo.LONG);
        FLOAT.addMethods(NumberInfo.FLOAT);
        DOUBLE.addMethods(NumberInfo.DOUBLE);
        OBJECT.addMethods(TypeInfo.OBJECT);
        POINTER.addField("getInstanceSize",PointerSize.INSTANCE);
        FUNCTION.addField("getInstanceSize",PointerSize.INSTANCE);
    }
    private void addMethods(TypeInfo instance){
        addField("getInstanceSize",new ValueExp(NumberInfo.MEMORY,"sizeof(" + instance.getCname() + ")"));
        addField("alloc",new Alloc(instance,false));
        addField("alloc[]",new Alloc(instance,true));
    }
    public ClassInfo(String name,String dataName){
        super(name + " class","",false);
        this.dataName = dataName;
    }
    public ClassInfo(String name){
        this(name,"");
    }
    public TypeInfo getInstanceType(){
        return instanceType;
    }
    public void setInstanceType(TypeInfo type){
        instanceType = type;
    }
    public String getDataName(){
        return dataName;
    }
}