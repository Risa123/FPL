package risa.fpl.env;

import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.info.InstanceInfo;

public abstract class ANameSpacedEnv extends SubEnv{
    private final StringBuilder initializer = new StringBuilder();
    private String initializerCall;
    private final StringBuilder functionCode = new StringBuilder(),functionDeclarations = new StringBuilder();
    protected final StringBuilder destructor = new StringBuilder();
    public ANameSpacedEnv(AEnv superEnv){
        super(superEnv);
    }
    public final void appendToInitializer(String code){
        initializer.append(code);
    }
    protected final String getInitializer(String name){
        if(initializer.isEmpty()){
            initializerCall = "";
        }else{
            var b = new StringBuilder("void ");
            initializerCall = IFunction.INTERNAL_METHOD_PREFIX + getNameSpace() + "_" + name;
            b.append(initializerCall);
            initializerCall += "();\n";
            b.append("(){\n").append(initializer).append("}\n");
            return b.toString();
        }
        return "";
    }
    public final String getInitializerCall(){
        return initializerCall;
    }
    public abstract String getNameSpace(IFunction caller);
    public abstract String getNameSpace();
    public String getFunctionCode(){
        return functionCode.toString();
    }
    public final String getFunctionDeclarations(){
        return functionDeclarations.toString();
    }
    public void appendFunctionCode(String code){
        functionCode.append(code);
    }
    public void appendFunctionDeclaration(Function func){
        functionDeclarations.append(func.getDeclaration());
    }
    public final void appendFunctionDeclarations(String code){
        functionDeclarations.append(code);
    }
    public void appendToDestructor(String code){
        destructor.append(code);
    }
    public final String getInitializerCode(){
        return initializer.toString();
    }
    public abstract void addTemplateInstance(InstanceInfo type);
}