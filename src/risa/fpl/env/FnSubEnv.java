package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.InterfaceInfo;
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
    public void compileDestructorCallsForReturn(StringBuilder builder,String returnedVariable){
        var superEnv = this.superEnv;
        var b = new StringBuilder();
        while(superEnv instanceof FnSubEnv env){
            env.compileDestructorCalls(b);
            superEnv = env.superEnv;
        }
        compileDestructorCalls(b);
        var code = b.toString();
        if(returnedVariable != null){
           if(getReturnTypeInternal() instanceof InstanceInfo i && i.getDestructorName() != null){
               code = code.replace(i.getDestructorName() +"(&" + returnedVariable + ");\n","");
           }else if(getReturnTypeInternal() instanceof InterfaceInfo){
               code = code.replace( "free(" + returnedVariable + "instance);\n","");
           }
        }
        builder.append(code);
    }
    public final void compileBlock(AExp exp,StringBuilder builder,ExpIterator it)throws CompilerException{
        var tmp = new StringBuilder();
        exp.compile(tmp,this,it);
        builder.append(toPointerVars).append(tmp);
        if(returnNotUsed){
            builder.append(destructorCalls);
        }
    }
    public final String getToPointerVars(){
        return toPointerVars.toString();
    }
    public final String getDestructorCalls(){
        return destructorCalls.toString();
    }
    public final void addInterfaceFreeCall(String cname){
        destructorCalls.append("free(").append(cname).append(".instance);\n");
    }
}