package risa.fpl.info;

import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.IField;

public final class CustomTypeInfo extends TypeInfo{
    private final TypeInfo original;
    private boolean complete;
    public CustomTypeInfo(String name,TypeInfo original,String declaration){
        super(name,IFunction.toCId(name));
        this.original = original;
        addRequiredType(original);
        appendToDeclaration("typedef ");
        appendToDeclaration(declaration);
        appendToDeclaration(";\n");
        buildDeclaration();
    }
    @Override
    public IField getField(String name,AEnv from){
        return original.getField(name,from);
    }
    @Override
    public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer){
        return original.ensureCast(to,expCode,comesFromPointer);
    }
    public boolean isComplete(){
        return complete;
    }
    @Override
    public void buildDeclaration(){
        complete = true;
        super.buildDeclaration();
    }
}