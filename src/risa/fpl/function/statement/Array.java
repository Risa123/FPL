package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
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
		writer.write(type.cname);
		var lenAtom = it.nextAtom();
	    if(lenAtom.type != TokenType.UINT && lenAtom.type != TokenType.ULONG) {
	    	throw new CompilerException(line,charNum,"array length expected instead of " + lenAtom.type);
	    }
	    var id = it.nextID();
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)) {
	    	cID = id.value;
	    	if(!IFunction.isCId(cID)) {
	    		throw new CompilerException(id,"invalid C identifier");
	    	}
	    }else {
	    	cID = IFunction.toCId(id.value);
	    }
	    writer.write(' ');
	    writer.write(cID);
	    writer.write('[');
	    writer.write(lenAtom.value);
	    writer.write(']');
	    if(it.hasNext()) {
	    	writer.write("={");
	    }
	    int count = 0;
	    var first = true;
	    env.addFunction(id.value,new Variable(new PointerInfo(type),cID,false,id.value,env.hasModifier(Modifier.CONST)));
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
		    var len = Long.parseLong(lenAtom.value);
		    writer.write('}');
		    if(count > len) {
		    	throw new CompilerException(line,charNum,"can only have " + len+  " elements");
		    }
	    }
		return TypeInfo.VOID;
	}

}