package risa.fpl.function.exp;

import risa.fpl.env.SubEnv;
import risa.fpl.env.IClassOwnedEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.CompilerException;

public final class GetObjectInfo extends AField implements ICalledOnPointer{
    private final TypeInfo returnType;
    private final String field;
    private final InstanceInfo self;
    private boolean calledOnPointer;
    public GetObjectInfo(TypeInfo returnType,String field,InstanceInfo self){
        this.returnType = returnType;
        this.field = field;
        this.self = self;
    }
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        var prev = new StringBuilder("((").append(self.getClassDataType()).append("*)");
        var calledOnThis = getPrevCode() == null && self.getClassInfo() == ((IClassOwnedEnv)env).getClassInfo();
        if(calledOnThis){
            prev.append("this");
        }
        writePrev(prev);
        if(calledOnPointer || calledOnThis){
            prev.append("->");
            calledOnPointer = false;
        }else{
            prev.append('.');
        }
        prev.append("objectData)->").append(field);
        return compileChainedCall(returnType,builder,env,it,prev.toString());
    }
    @Override
    public void calledOnPointer(){
        calledOnPointer = true;
    }
}