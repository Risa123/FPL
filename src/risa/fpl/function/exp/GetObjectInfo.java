package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

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
    public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException{
        writer.write("((" + self.getClassDataType());
        writePrev(writer);
        if(!calledOnPointer){
            writer.write(')');
            if(getPrevCode() == null){
                writer.write("this");
            }
            writer.write("->");
        }else{
            calledOnPointer = false;
            writer.write("&).");
        }
        writer.write("object_data)->" + field);
        return  returnType;
    }
    @Override
    public void calledOnPointer() {
        calledOnPointer = true;
    }
}