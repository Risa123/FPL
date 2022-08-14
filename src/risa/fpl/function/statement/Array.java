package risa.fpl.function.statement;


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
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
		env.checkModifiers(line,tokenNum,Modifier.CONST);
		var b = new StringBuilder();
		if(env.hasModifier(Modifier.CONST)){
			b.append("const ");
		}
		var typeAtom = it.nextID();
		var type = env.getType(typeAtom);
		if(env instanceof ClassEnv e && e.getInstanceInfo() == type){
		    b.append("struct ");
        }
		var lenAtom = it.nextAtom();
		if(lenAtom.getType() == AtomType.END_ARGS){
		    type = generateTypeFor(type,typeAtom,it,env,false);
		    lenAtom = it.nextAtom();
        }
        b.append(type.getCname());
	    if(lenAtom.notIndexLiteral()){
	    	error(lenAtom,"array length expected instead of " + lenAtom);
	    }
	    var id = it.nextID();
		if(env instanceof ClassEnv e){
			e.getVariableFieldDeclarationOrder().add(id.getValue());
		}
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)){
	    	cID = id.getValue();
	    	if(IFunction.notCID(cID)){
	    		error(id,"invalid C identifier");
	    	}
	    }else{
	    	cID = IFunction.toCId(id.getValue());
	    }
	    b.append(' ').append(cID).append('[').append(lenAtom.getValue()).append(']');
	    if(it.hasNext()){
	    	b.append("={");
	    }
	    int count = 0;
	    var first = true;
	    var instanceType = env instanceof ClassEnv e?e.getInstanceInfo():null;
		var len = lenAtom.getType() == AtomType.ULONG?Long.parseUnsignedLong(lenAtom.getValue()):Long.parseLong(lenAtom.getValue());
	    var v = new Variable(new ArrayInfo(type,len,lenAtom.getType() == AtomType.ULONG),cID,false,id.getValue(),env.hasModifier(Modifier.CONST),instanceType,env.getAccessModifier());
	    env.addFunction(id.getValue(),v);
	    if(it.hasNext()){
	    	while(it.hasNext()){
		    	var exp = it.nextAtom();
		        if(exp.getType() != AtomType.ARG_SEPARATOR){
					if(first){
						first = false;
					}else{
						b.append(',');
					}
					var buffer = new StringBuilder();
					var expType = exp.compile(buffer,env,it);
					if(!expType.equals(type)){
						throw new CompilerException(exp,type + " expected instead of " + expType);
					}
					b.append(expType.ensureCast(type,buffer.toString(),env));
					count++;
				}
		    }
		    b.append("};\n");
		    if(count > len){
		    	error(line,tokenNum,"can only have " + len + " elements");
		    }
	    }
		if(env instanceof ModuleEnv e){
			e.appendVariableDeclaration(b.toString());
		}else{
			builder.append(b);
		}
		return TypeInfo.VOID;
	}
}