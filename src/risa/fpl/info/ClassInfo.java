package risa.fpl.info;

public final class ClassInfo extends TypeInfo{
    public static final ClassInfo CHAR = new ClassInfo("char");
    public static final ClassInfo BOOL = new ClassInfo("bool");
    public static final ClassInfo STRING = new ClassInfo("string");
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
    static{
        TypeInfo.CHAR.setClassInfo(CHAR);
        TypeInfo.BOOL.setClassInfo(BOOL);
        TypeInfo.STRING.setClassInfo(STRING);
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
    }
    public ClassInfo(String name){
        super(name + " class","",false);
    }
}