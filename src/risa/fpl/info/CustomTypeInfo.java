package risa.fpl.info;

import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.AField;

public final class CustomTypeInfo extends TypeInfo{
    private final TypeInfo original;
    private final String declaration;
    public CustomTypeInfo(String name,TypeInfo original,String declaration){
        super(name,IFunction.toCId(name));
        while(original instanceof CustomTypeInfo t){
            original = t.original;
        }
        this.original = original;
        this.declaration = "typedef " + declaration + ";\n";
    }
    @Override
    public AField getField(String name,AEnv from){
        return original.getField(name,from);
    }
    @Override
    public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer){
        return original.ensureCast(to,expCode,comesFromPointer);
    }
    @Override
    public String getDeclaration(){
        return declaration;
    }
    @Override
    public boolean isPrimitive(){
        return false;
    }
}