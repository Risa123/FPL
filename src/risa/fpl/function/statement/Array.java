package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.Modifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class Array implements IFunction {

	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		if(env.hasModifier(Modifier.CONST)) {
			writer.write("const ");
		}
		var type = env.getType(it.nextID());
		if(env instanceof ClassEnv e && e.getInstanceType() == type){
		    writer.write("struct ");
        }
		writer.write(type.getCname());
		var lenAtom = it.nextAtom();
	    if(lenAtom.getType() != TokenType.UINT && lenAtom.getType() != TokenType.ULONG) {
	    	throw new CompilerException(line,charNum,"array length expected instead of " + lenAtom);
	    }
	    var id = it.nextID();
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)) {
	    	cID = id.getValue();
	    	if(!IFunction.isCId(cID)) {
	    		throw new CompilerException(id,"invalid C identifier");
	    	}
	    }else {
	    	cID = IFunction.toCId(id.getValue());
	    }
	    writer.write(' ');
	    writer.write(cID);
	    writer.write('[');
	    writer.write(lenAtom.getValue());
	    writer.write(']');
	    if(it.hasNext()) {
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
	    if(it.hasNext()) {
	    	while(it.hasNext()) {
		    	var exp = it.next();
		    	if(first) {
		    		first = false;
		    	}else {
		    		writer.write(',');
		    	}
		    	var expType = exp.compile(writer, env, it);
		    	if(!expType.equals(type)){
		    		throw new CompilerException(exp,type + " expected instead of " + expType );
		    	}
		    	count++;
		    }
		    var len = Long.parseLong(lenAtom.getValue());
		    writer.write('}');
		    if(count > len) {
		    	throw new CompilerException(line,charNum,"can only have " + len+  " elements");
		    }
	    }
		return TypeInfo.VOID;
	}

}