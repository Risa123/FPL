package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
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
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)) {
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
			appendSemicolon = true;
		}
        var f = new Function(id.getValue(),returnType,cID,args,env.hasModifier(Modifier.NATIVE),owner,env.getAccessModifier(),env);
        Modifier type = null;
        if(env.hasModifier(Modifier.ABSTRACT)){
            type = Modifier.ABSTRACT;
        }else if(env.hasModifier(Modifier.VIRTUAL)){
            type = Modifier.VIRTUAL;
        }
        if(type != null){
            f.setType(type);
        }
        var p = new PointerInfo(f);
        if(env instanceof ClassEnv cEnv){
           cEnv.addMethod(f,b.getText());
        }else if(env.hasModifier(Modifier.ABSTRACT)){
            writer.write(p.getFunctionPointerDeclaration(cID));
        }else{
            writer.write(b.getText());
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