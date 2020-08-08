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
	private final TypeInfo target;
    public Cast(TypeInfo target) {
    	this.target = target;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
	    var type = env.getType(it.nextID());
	    if((target instanceof NumberInfo || target instanceof PointerInfo) && (type instanceof  NumberInfo || type instanceof PointerInfo)){
            writer.write('(');
            writer.write(type.getCname());
            writer.write(')');
            writePrev(writer);
        }else if(isCharOrNumber(target) && isCharOrNumber(type)){
	        writer.write("(char)");
        }else if(!type.isPrimitive() && target instanceof InterfaceInfo && type.getParents().contains(target)){
          writer.write('(');
          writer.write(type.getCname());
          writer.write(')');
          writePrev(writer);
          writer.write(".instance");
        }else{
	        throw new CompilerException(line,charNum,"cannot cast " + target + " to " + type);
        }
		return type;
	}
    private boolean isCharOrNumber(TypeInfo type){
        return type == TypeInfo.CHAR || type instanceof NumberInfo n && n.isFloatingPoint();
    }
}