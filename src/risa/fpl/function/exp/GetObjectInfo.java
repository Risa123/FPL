package risa.fpl.function.exp;

import risa.fpl.BuilderWriter;
import risa.fpl.env.AEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.CompilerException;

import java.io.BufferedWriter;
import java.io.IOException;

public final class GetObjectInfo extends AField implements  ICalledOnPointer{
    private final TypeInfo returnType;
    private final String field;
    private final InstanceInfo self;
    private boolean calledOnPointer;
    public GetObjectInfo(TypeInfo returnType, String field, InstanceInfo self){
        this.returnType = returnType;
        this.field = field;
        this.self = self;
    }
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        var prev = new BuilderWriter(writer);
        prev.write("((" + self.getClassDataType());
        writePrev(prev);
        if(!calledOnPointer){
            prev.write(')');
            if(getPrevCode() == null){
                prev.write("this");
            }
            prev.write("->");
        }else{
            calledOnPointer = false;
            prev.write("&).");
        }
        prev.write("object_data)->" + field);
        return  compileChainedCall(returnType,writer,env,it,prev.getCode());
    }
    @Override
    public void calledOnPointer() {
        calledOnPointer = true;
    }
}