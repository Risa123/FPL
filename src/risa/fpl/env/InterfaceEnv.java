package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.AField;
import risa.fpl.function.statement.Var;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.parser.Atom;

public final class InterfaceEnv extends SubEnv{
    private final InterfaceInfo type;
    public InterfaceEnv(ModuleEnv module,String id){
        super(module);
        type = new InterfaceInfo(module,id);
        addModifier(Modifier.ABSTRACT);
    }
    @Override
    public IFunction getFunction(Atom atom)throws CompilerException{
        var f = super.getFunction(atom);
        if(f instanceof Var){
            throw new CompilerException(atom,"variables cannot be declared in interface");
        }
        return f;
    }
    @Override
    public void addFunction(String name,IFunction value){
        type.addField(name,(AField) value);
    }
    public InterfaceInfo getType(){
        return type;
    }
}