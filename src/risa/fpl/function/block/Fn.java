package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.env.Modifier;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class Fn extends AFunctionBlock{
	private boolean appendSemicolon;
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum) throws IOException, CompilerException {
		var returnTypeAtom = it.nextID();
		var returnType = env.getType(returnTypeAtom);
		if(env.hasModifier(Modifier.NATIVE)) {
			writer.write("extern ");
		}
		writer.write(returnType.cname);
		writer.write(' ');
		var id = it.nextID();
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)) {
	    	cID = id.value;
	    	if(!IFunction.isCId(id.value)) {
	    		throw new CompilerException(id,"invalid C identifier");
	    	}
	    }else {
	    	cID = "";
	    	if(env instanceof ModuleEnv mod) {
	    		cID = mod.getNameSpace();
	    	}
	    	cID += IFunction.toCId(id.value);
	    }
		writer.write(cID);
		writer.write('(');
		var fnEnv = new FnEnv(env,returnType);
		var f = new Function(id.value,returnType,cID, parseArguments(writer,it,fnEnv),env.hasModifier(Modifier.NATIVE),null);
		var p = new PointerInfo(f);
		env.addFunction("&" + id.value,new ValueExp(p,"&" + cID));
		env.addFunction(id.value,f);
		env.addType(id.value,p,false);
		writer.write(')');
		if(it.hasNext()) {
			writer.write("{\n");
			var block = it.nextList();
			block.compile(writer,fnEnv,it);
			if(!fnEnv.isReturnUsed() && returnType != TypeInfo.VOID) {
				throw new CompilerException(block,"there is no return in this block and this function doesn't return void");
			}
			writer.write("}\n");
		}else {
			appendSemicolon = true;
		}
		return TypeInfo.VOID;
	}
	@Override
	public boolean appendSemicolon() {
		return appendSemicolon;
	}
}