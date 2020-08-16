package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public class Fn extends AFunctionBlock {
	private boolean appendSemicolon;
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum) throws IOException, CompilerException {
	    var b = new BuilderWriter(writer);
		var returnTypeAtom = it.nextID();
		if(returnTypeAtom.getValue().equals("<>")){ //generic function
		    returnTypeAtom = it.nextID();
        }
		var returnType = env.getType(returnTypeAtom);
		if(env.hasModifier(Modifier.NATIVE)) {
			b.write("extern ");
		}
		b.write(returnType.getCname());
		b.write(' ');
		var id = it.nextID();
        if(env instanceof  ModuleEnv e && e.isMain() && id.getValue().equals("main")){
           throw new CompilerException(id,"main method can only be declared using build-in function main");
        }
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE) || env.hasModifier(Modifier.ABSTRACT)) {
	    	cID = id.getValue();
	    	if(!IFunction.isCId(id.getValue())) {
	    		throw new CompilerException(id,"invalid C identifier");
	    	}
	    }else {
	    	cID = "";
	    	if(env instanceof ANameSpacedEnv tmp) {
	    		cID = tmp.getNameSpace(this);
	    	}
	    	cID += IFunction.toCId(id.getValue());
	    }
		b.write(cID);
        TypeInfo owner = null;
        ClassInfo classType = null;
        if(env instanceof  ClassEnv cEnv){
            owner = cEnv.getInstanceType();
            classType = owner.getClassInfo();
        }else if(env instanceof InterfaceEnv e){
            owner = e.getType();
        }
        var fnEnv = new FnEnv(env,returnType,classType);
		var args = parseArguments(b,it,fnEnv,owner);
		if(it.hasNext()) {
		    if(env.hasModifier(Modifier.ABSTRACT)){
		        throw new CompilerException(line,charNum,"abstract methods can only be declared");
            }
			b.write("{\n");
			var block = it.nextList();
			block.compile(b,fnEnv,it);
			if(!fnEnv.isReturnUsed() && returnType != TypeInfo.VOID) {
				throw new CompilerException(block,"there is no return in this block and this function doesn't return void");
			}
			b.write("}\n");
		}else {
		    if(!(env.hasModifier(Modifier.ABSTRACT) || env.hasModifier(Modifier.NATIVE))){
		        throw new CompilerException(line,charNum,"block required");
            }
			appendSemicolon = true;
		}
        FunctionType type;
        if(env.hasModifier(Modifier.ABSTRACT)){
            type = FunctionType.ABSTRACT;
            appendSemicolon = false;
        }else if(env.hasModifier(Modifier.VIRTUAL)){
            type = FunctionType.VIRTUAL;
        }else{
            type = FunctionType.NORMAL;
        }
        var f = new Function(id.getValue(),returnType,cID,args,type,owner,env.getAccessModifier(),env);
        if(owner != null){
            var parentField = owner.getField(id.getValue(),env);
            if(env.hasModifier(Modifier.OVERRIDE)){
                if(!(parentField instanceof Function parentMethod)){
                    throw new CompilerException(line,charNum,"there is no method " + id + " to override");
                }
                if(!parentMethod.equalSignature(f)){
                    throw new CompilerException(line,charNum,"this method doesn´t have signature of one it overrides");
                }
            }else{
                if(parentField != null){
                    throw new CompilerException(line,charNum,"override is required");
                }
            }
        }
        var p = new PointerInfo(f);
        if(env instanceof ClassEnv cEnv){
            cEnv.addMethod(f,b.getCode());
        }else if(env instanceof InterfaceEnv){
            writer.write(p.getFunctionPointerDeclaration(cID) + ";\n");
        }else{
            writer.write(b.getCode());
        }
        env.addFunction("&" + id,new ValueExp(p,"&" + cID));
        env.addFunction(id.getValue(),f);
        env.addType(id.getValue(),p,false);
		return TypeInfo.VOID;
	}
	@Override
	public boolean appendSemicolon() {
		return appendSemicolon;
	}
}