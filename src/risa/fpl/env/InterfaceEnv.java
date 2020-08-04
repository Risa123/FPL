package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.function.IFunction;
import risa.fpl.function.statement.Var;
import risa.fpl.parser.Atom;

public final class InterfaceEnv extends SubEnv {
    public InterfaceEnv(AEnv superEnv) {
        super(superEnv);
        addModifier(Modifier.ABSTRACT);
    }
    @Override
    public IFunction getFunction(Atom atom) throws CompilerException {
        var f = super.getFunction(atom);
        if(f instanceof Var){
            throw new CompilerException(atom,"variables cannot be declared in interface");
        }
        return f;
    }
}