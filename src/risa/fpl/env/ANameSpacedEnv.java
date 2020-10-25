package risa.fpl.env;

import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;

public abstract class ANameSpacedEnv extends SubEnv {
    private final StringBuilder initializer = new StringBuilder();
    private String initializerName;
    private final StringBuilder functionCode = new StringBuilder(), functionDeclaration = new StringBuilder();
    public ANameSpacedEnv(AEnv superEnv) {
        super(superEnv);
    }
    public final void appendToInitializer(String code){
        initializer.append(code);
    }
    public final String getInitializer(String name){
        var b = new StringBuilder("void ");
        b.append(IFunction.INTERNAL_METHOD_PREFIX);
        b.append(name).append(getNameSpace());
        initializerName = IFunction.INTERNAL_METHOD_PREFIX + getNameSpace() + name +"();\n";
        b.append("(){\n").append(initializer.toString());
        b.append("}\n");
        return b.toString();
    }
    public final String getInitializerCall(){
        return initializerName;
    }
    public abstract String getNameSpace(IFunction caller);
    public abstract String getNameSpace();
    public String getFunctionCode(){
        return functionCode.toString();
    }
    public String getFunctionDeclaration(){
        return functionDeclaration.toString();
    }
    public void appendFunctionCode(String code){
        functionCode.append(code);
    }
    public void appendFunctionDeclaration(Function func){
        functionDeclaration.append(func.getDeclaration()).append(";\n");
    }
}