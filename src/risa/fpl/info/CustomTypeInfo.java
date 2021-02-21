package risa.fpl.info;

import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.IField;

public final class CustomTypeInfo extends TypeInfo{
    private final TypeInfo original;
    public CustomTypeInfo(String name,TypeInfo original,String declaration){
        super(name,IFunction.toCId(name),original.isPrimitive());
        this.original = original;
        addRequiredType(original);
        appendToDeclaration("typedef ");
        appendToDeclaration(declaration);
        appendToDeclaration(' ');
        appendToDeclaration(getCname());
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
}