package risa.fpl.function;

import risa.fpl.env.AEnv;

public final class SetAccessModifier extends AddModifier{
    public SetAccessModifier(AccessModifier mod){
        super(mod);
    }
    @Override
    public void removeMod(AEnv env){
        env.setAccessModifier(AccessModifier.PUBLIC);
    }
    @Override
    public void addMod(AEnv env){
        env.setAccessModifier((AccessModifier)mod);
    }
}