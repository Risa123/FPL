package risa.fpl.info;

public final class ClassInfo extends TypeInfo{
    public static final ClassInfo CHAR_CLASS = new ClassInfo("char");
    public static final ClassInfo BOOL_CLASS = new ClassInfo("bool");
    public static final ClassInfo STRING_CLASS = new ClassInfo("string");
    public ClassInfo(String name) {
        super(name + " class","");
    }
}