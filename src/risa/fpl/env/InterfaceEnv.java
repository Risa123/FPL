package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.IField;
import risa.fpl.function.statement.Var;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.parser.Atom;

public final class InterfaceEnv extends SubEnv {
    private final InterfaceInfo type;
    public InterfaceEnv(AEnv superEnv,String id) {
        super(superEnv);
        addModifier(Modifier.ABSTRACT);
        type = new InterfaceInfo(id);
    }
    @Override
    public IFunction getFunction(Atom atom) throws CompilerException {
        var f = super.getFunction(atom);
        if(f instanceof Var){
            throw new CompilerException(atom,"variables cannot be declared in interface");
        }
        return f;
    }
    @Override
    public void addFunction(String name, IFunction value) {
        type.addField(name,(IField)value);
    }
    public InterfaceInfo getType(){
        return type;
    }
}