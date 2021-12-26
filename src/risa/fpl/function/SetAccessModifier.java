package risa.fpl.function;

import risa.fpl.CompilerException;
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
    public void addModifier(SubEnv env,int line,int tokenNum)throws CompilerException{
        if(env.getAccessModifier() == mod){
            throw new CompilerException(line,tokenNum,"duplicate modifier " + mod.toString().toLowerCase());
        }
        env.setAccessModifier((AccessModifier)mod);
    }
}