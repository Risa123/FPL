package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.ModuleEnv;
import risa.fpl.env.SubEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.Modifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.ArrayInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

public final class Array implements IFunction{
	@Override
	public TypeInfo compile(BufferedWriter writer,SubEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		var b = new BuilderWriter();
		if(env.hasModifier(Modifier.CONST)){
			b.write("const ");
		}
		var typeAtom = it.nextID();
		var type = env.getType(typeAtom);
		if(env instanceof ClassEnv e && e.getInstanceInfo() == type){
		    b.write("struct ");
        }
		var lenAtom = it.nextAtom();
		if(lenAtom.getType() == AtomType.END_ARGS){
		    type = IFunction.generateTypeFor(type,typeAtom,it,env,false);
		    lenAtom = it.nextAtom();
        }
        b.write(type.getCname());
	    if(lenAtom.notIndexLiteral()){
	    	throw new CompilerException(lenAtom,"array length expected instead of " + lenAtom);
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
	    b.write(' ' + cID + '[' + lenAtom.getValue() + ']');
	    if(it.hasNext()){
	    	b.write("={");
	    }
	    int count = 0;
	    var first = true;
	    TypeInfo instanceType = null;
	    if(env instanceof ClassEnv e){
	        instanceType = e.getInstanceInfo();
        }
		long len;
		if(lenAtom.getType() == AtomType.ULONG){
            len = Long.parseUnsignedLong(lenAtom.getValue());
		}else{
			len = Long.parseLong(lenAtom.getValue());
		}
	    var v = new Variable(new ArrayInfo(type,len,lenAtom.getType() == AtomType.ULONG),cID,false,id.getValue(),env.hasModifier(Modifier.CONST),instanceType,env.getAccessModifier());
	    env.addFunction(id.getValue(),v);
	    if(it.hasNext()){
	    	while(it.hasNext()){
		    	var exp = it.nextAtom();
		        if(exp.getType() != AtomType.ARG_SEPARATOR){
					if(first){
						first = false;
					}else{
						b.write(',');
					}
					var buffer = new BuilderWriter();
					var expType = exp.compile(buffer,env,it);
					if(!expType.equals(type)){
						throw new CompilerException(exp,type + " expected instead of " + expType);
					}
					b.write(expType.ensureCast(type,buffer.getCode()));
					count++;
				}
		    }
		    b.write("};\n");
		    if(count > len){
		    	throw new CompilerException(line,tokenNum,"can only have " + len + " elements");
		    }
	    }
		if(env instanceof ModuleEnv e){
			e.appendVariableDeclaration(b.getCode());
		}else{
			writer.write(b.getCode());
		}
		return TypeInfo.VOID;
	}
}