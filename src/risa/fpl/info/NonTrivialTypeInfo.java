package risa.fpl.info;

import risa.fpl.env.ModuleEnv;
import risa.fpl.function.exp.AField;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;

public abstract class NonTrivialTypeInfo extends TypeInfo{
    private final StringBuilder declarationBuilder = new StringBuilder();
    protected final ModuleEnv module;
    public NonTrivialTypeInfo(ModuleEnv module,String name,String cname){
        super(name,cname);
        this.module = module;
        setClassInfo(new ClassInfo(name));
    }
    public void buildDeclaration(){}
    protected final void addFunctionRequiredTypes(Function f){
        for(var t:f.getRequiredTypes()){
            addRequiredType(t);
        }
    }
    public final void appendToDeclaration(String code){
        declarationBuilder.append(code);
    }
    @Override
    public  String getDeclaration(){
        return declarationBuilder.toString();
    }
    @Override
    public boolean isPrimitive(){
        return false;
    }
    @Override
    public void addParent(TypeInfo parent){
        super.addParent(parent);
        addRequiredType(parent);
    }
    @Override
    public void addField(String name,AField field){
        super.addField(name,field);
        if(field instanceof Variable v){
            addRequiredType(v.getType());
        }else if(field instanceof Function f){
            addFunctionRequiredTypes(f);
        }
    }
}