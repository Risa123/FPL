package risa.fpl.env;

import risa.fpl.function.IFunction;
import risa.fpl.function.block.ExpressionInfo;
import risa.fpl.function.exp.Function;
import risa.fpl.info.InstanceInfo;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class ANameSpacedEnv extends SubEnv{
    private final StringBuilder initializer = new StringBuilder();
    private String initializerCall;
    private final StringBuilder functionCode = new StringBuilder(),functionDeclarations = new StringBuilder();
    protected final StringBuilder destructor = new StringBuilder();
    private final HashMap<Integer,ArrayList<ExpressionInfo>>modifierBlockInfos = new HashMap<>();
    private final HashMap<Integer,ArrayList<ExpressionInfo>>compileIfBlockInfos = new HashMap<>();
    public ANameSpacedEnv(AEnv superEnv){
        super(superEnv);
    }
    public final void appendToInitializer(String code){
        initializer.append(code);
    }
    protected String getInitializer(String name){
        if(initializer.isEmpty()){
            initializerCall = "";
        }else{
            var b = new StringBuilder("void ");
            var cname = IFunction.INTERNAL_METHOD_PREFIX + getNameSpace() + "_" + name;
            b.append(cname);
            initializerCall = "void " + cname + "();\n" + cname + "();";
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
    public final void appendFunctionDeclaration(String code){
        functionDeclarations.append(code);
    }
    public void appendToDestructor(String code){
        destructor.append(code);
    }
    public final String getInitializerCode(){
        return initializer.toString();
    }
    public final void addModifierBlockInfos(int line, ArrayList<ExpressionInfo> infos){
        modifierBlockInfos.put(line,infos);
    }
    public final ArrayList<ExpressionInfo>getModifierBlockInfos(int line){
        return modifierBlockInfos.get(line);
    }
    public final void addCompileIfBlockInfos(int line, ArrayList<ExpressionInfo> block){
        compileIfBlockInfos.put(line,block);
    }
    public final ArrayList<ExpressionInfo>getCompileBlockInfos(int line){
        return compileIfBlockInfos.get(line);
    }
    public abstract void addTemplateInstance(InstanceInfo type);
}