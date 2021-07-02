package risa.fpl.function.exp;

import risa.fpl.BuilderWriter;
import risa.fpl.env.AEnv;
import risa.fpl.env.IClassOwnedEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.CompilerException;

import java.io.BufferedWriter;
import java.io.IOException;

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
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var prev = new BuilderWriter(writer);
        prev.write("((" + self.getClassDataType() + ")");
        var calledOnThis = getPrevCode() == null && self.getClassInfo() == ((IClassOwnedEnv)env).getClassType();
        if(calledOnThis){
            prev.write("this");
        }
        writePrev(prev); //prev code not null
        if(calledOnPointer || calledOnThis){
            prev.write("->");
            calledOnPointer = false;
        }else{
            prev.write('.');
        }
        prev.write("objectData)->" + field);
        return  compileChainedCall(returnType,writer,env,it,prev.getCode());
    }
    @Override
    public void calledOnPointer(){
        calledOnPointer = true;
    }
}