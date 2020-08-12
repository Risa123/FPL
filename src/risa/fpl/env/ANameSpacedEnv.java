package risa.fpl.env;

import risa.fpl.function.IFunction;

public abstract class ANameSpacedEnv extends SubEnv {
    private final StringBuilder initializer = new StringBuilder();
    private String initializerName;
    public ANameSpacedEnv(AEnv superEnv) {
        super(superEnv);
    }
    public final void appendToInitializer(String code){
        initializer.append(code);
    }
    public final String getInitializer(String name){
        var b = new StringBuilder("void ");
        b.append(IFunction.INTERNAL_METHOD_PREFIX);
        b.append(getNameSpace());
        b.append(name);
        initializerName = IFunction.INTERNAL_METHOD_PREFIX + getNameSpace() + name +"();\n";
        b.append("(){\n");
        b.append(initializer.toString());
        b.append("}\n");
        return b.toString();
    }
    public final String getInitializerCall(){
        return initializerName;
    }
    public abstract String getNameSpace(IFunction caller);
    public abstract String getNameSpace();
}