package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.ExpIterator;

public class FnSubEnv extends SubEnv implements IClassOwnedEnv{
    protected boolean returnNotUsed = true;
    public FnSubEnv(AEnv superEnv){
        super(superEnv);
    }
    @Override
    public final ClassInfo getClassInfo(){
        return superEnv instanceof IClassOwnedEnv e?e.getClassInfo():null;
    }
    public final void addTemplateInstance(InstanceInfo type){
        if(superEnv instanceof ANameSpacedEnv e){
            e.addTemplateInstance(type);
        }else{
            ((FnSubEnv)superEnv).addTemplateInstance(type);
        }
    }
    public final boolean isReturnNotUsed(){
        return returnNotUsed;
    }
    public TypeInfo getReturnType(){
        returnNotUsed = false;
        return ((FnSubEnv)superEnv).getReturnTypeInternal();
    }
    protected TypeInfo getReturnTypeInternal(){
        return ((FnSubEnv)superEnv).getReturnTypeInternal();
    }
    public boolean isInMainBlock(){
        return ((FnSubEnv)superEnv).isInMainBlock();
    }
    public void compileDestructorCallsFromWholeFunction(StringBuilder builder){
        var superEnv = this.superEnv;
        while(superEnv instanceof FnSubEnv env){
            env.compileDestructorCalls(builder);
            superEnv = env.superEnv;
        }
        compileDestructorCalls(builder);
    }
    public final void compileBlock(AExp exp,StringBuilder builder,ExpIterator it)throws CompilerException{
        var tmp = new StringBuilder();
        exp.compile(tmp,this,it);
        builder.append(toPointerVars).append(tmp).append(destructorCalls);
    }
}