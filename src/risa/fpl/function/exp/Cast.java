package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Cast extends AField {
	private final TypeInfo self;
    public Cast(TypeInfo self) {
    	this.self = self;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum) throws IOException, CompilerException {
	    var type = env.getType(it.nextID());
	    if((self instanceof NumberInfo || self instanceof PointerInfo) && (type instanceof  NumberInfo || type instanceof PointerInfo)){
	        C_cast(writer,type.getCname());
        }else if(isCharOrNumber(self) && isCharOrNumber(type)){
	        C_cast(writer,"char");
        }else if(!type.isPrimitive() && self instanceof InterfaceInfo && type.getParents().contains(self)){
          C_cast(writer,type.getCname());
          writer.write(".instance");
        }else{
	        var npTarget = self;
	        var npType = type;
	        if(npTarget instanceof PointerInfo p){
	            npTarget = p.getType();
            }
	        if(npType instanceof PointerInfo p){
	            npType = p.getType();
            }
	        if(npTarget.getParents().contains(npType)){
	            writer.write("(" + type.getCname() + ")");
	            if(!(self instanceof PointerInfo)){
	                writer.write('&');
                }
	            writePrev(writer);
	            return type;
            }
	        throw new CompilerException(line,charNum,"cannot cast " + self + " to " + type);
        }
		return type;
	}
    private boolean isCharOrNumber(TypeInfo type){
        return type == TypeInfo.CHAR || type instanceof NumberInfo n && !n.isFloatingPoint();
    }
    private void C_cast(BufferedWriter writer,String c_type) throws IOException {
        writer.write("((" + c_type + ")");
        writePrev(writer);
        writer.write(')');
    }
}