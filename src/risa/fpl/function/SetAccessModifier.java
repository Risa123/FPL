package risa.fpl.function;

import risa.fpl.env.SubEnv;

public final class SetAccessModifier extends AddModifier{
    public SetAccessModifier(AccessModifier mod){
        super(mod);
    }
    @Override
    public void removeModifier(SubEnv env){
        env.setAccessModifier(AccessModifier.PUBLIC);
    }
    @Override
    public void addModifier(SubEnv env){
        env.setAccessModifier((AccessModifier)mod);
    }
}