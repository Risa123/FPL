package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.Modifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

public final class Array implements IFunction{
	@Override
	public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		if(env.hasModifier(Modifier.CONST)){
			writer.write("const ");
		}
		var typeAtom = it.nextID();
		var type = env.getType(typeAtom);
		if(env instanceof ClassEnv e && e.getInstanceType() == type){
		    writer.write("struct ");
        }
		var lenAtom = it.nextAtom();
		if(lenAtom.getType() == AtomType.END_ARGS){
		    type = IFunction.generateTypeFor(type,typeAtom,it,env,false);
		    lenAtom = it.nextAtom();
        }
        writer.write(type.getCname());
	    if(lenAtom.notIndexLiteral()){
	    	throw new CompilerException(line, tokenNum,"array length expected instead of " + lenAtom);
	    }
	    var id = it.nextID();
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)){
	    	cID = id.getValue();
	    	if(IFunction.notCID(cID)){
	    		throw new CompilerException(id,"invalid C identifier");
	    	}
	    }else{
	    	cID = IFunction.toCId(id.getValue());
	    }
	    writer.write(' ');
	    writer.write(cID);
	    writer.write('[');
	    writer.write(lenAtom.getValue());
	    writer.write(']');
	    if(it.hasNext()){
	    	writer.write("={");
	    }
	    int count = 0;
	    var first = true;
	    TypeInfo instanceType = null;
	    if(env instanceof ClassEnv e){
	        instanceType = e.getInstanceType();
        }
	    var v = new Variable(new PointerInfo(type),cID,false,id.getValue(),env.hasModifier(Modifier.CONST),instanceType,env.getAccessModifier());
	    env.addFunction(id.getValue(),v);
	    if(it.hasNext()){
	    	while(it.hasNext()){
		    	var exp = it.nextAtom();
		        if(exp.getType() != AtomType.ARG_SEPARATOR){
					if(first){
						first = false;
					}else{
						writer.write(',');
					}
					var buffer = new BuilderWriter();
					var expType = exp.compile(buffer,env,it);
					if(!expType.equals(type)){
						throw new CompilerException(exp,type + " expected instead of " + expType);
					}
					writer.write(expType.ensureCast(type,buffer.getCode()));
					count++;
				}
		    }
		    var len = Long.parseLong(lenAtom.getValue());
		    writer.write('}');
		    if(count > len){
		    	throw new CompilerException(line, tokenNum,"can only have " + len +  " elements");
		    }
	    }
		return TypeInfo.VOID;
	}
}