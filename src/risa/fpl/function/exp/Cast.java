package risa.fpl.function.exp;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.function.IFunction;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Cast extends AField{
	private final TypeInfo self;
    public Cast(TypeInfo self){
    	this.self = self;
    }
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        var typeAtom = it.nextID();
	    var type = env.getType(typeAtom);
	    var prev = new StringBuilder();
	    if(it.checkTemplate()){
            type = IFunction.generateTypeFor(type,typeAtom,it,env,false);
        }
	    if((self instanceof NumberInfo || self instanceof PointerInfo) && (type instanceof  NumberInfo || type instanceof PointerInfo)){
	        CCast(prev,type.getCname());
        }else if(isCharOrNumber(self) && isCharOrNumber(type) || (self == TypeInfo.CHAR && type instanceof NumberInfo)){
	        CCast(prev,"char");
        }else if(!type.isPrimitive() && self instanceof InterfaceInfo && type.getParents().contains(self)){
          CCast(prev,type.getCname());
          prev.append(".instance");
        }else if(type instanceof InterfaceInfo){
	        throw new CompilerException(line, tokenNum,"cannot cast to interface");
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
	            prev.append("(").append(type.getCname()).append(")");
	            if(!(self instanceof PointerInfo)){
	                prev.append('&');
                }
	            writePrev(prev);
	            return compileChainedCall(type,builder,env,it,prev.toString());
            }
	        throw new CompilerException(line,tokenNum,"cannot cast " + self + " to " + type);
        }
		return compileChainedCall(type,builder,env,it,prev.toString());
	}
    private boolean isCharOrNumber(TypeInfo type){
        return type == TypeInfo.CHAR || type instanceof NumberInfo n && !n.isFloatingPoint();
    }
    private void CCast(StringBuilder builder,String ctype){
		builder.append("((").append(ctype).append(")");
        writePrev(builder);
        builder.append(')');
    }
}