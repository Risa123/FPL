package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public class Fn extends ABlock {
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
		var fnEnv = new FnEnv(env,returnType);
        TypeInfo owner = null;
        if(env instanceof  ClassEnv cEnv){
            owner = cEnv.getInstanceType();
        }
		var args = parseArguments(b,it,fnEnv,owner);
		if(it.hasNext()) {
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
        var f = new Function(id.getValue(),returnType,cID,args,env.hasModifier(Modifier.NATIVE),owner);
        var p = new PointerInfo(f);
        if(env instanceof ClassEnv cEnv){
           cEnv.addMethod(id.getValue(),f,b.getText());
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
    TypeInfo[]parseArguments(BufferedWriter writer,ExpIterator it,FnEnv env,TypeInfo owner) throws CompilerException, IOException{
        writer.write('(');
        var args = new ArrayList<TypeInfo>();
        var first = owner == null;
        if(!first){
            writer.write(owner.getCname());
            writer.write("* this");
            env.addFunction("this",new Variable(new PointerInfo(owner),"this","this"));
        }
        while(it.hasNext()) {
            if(it.peek() instanceof List) {
                break;
            }
            if(first) {
                first = false;
            }else {
                writer.write(',');
            }
            var argType = env.getType(it.nextID());
            args.add(argType);
            var argName = it.nextID();
            var argNameCID = IFunction.toCId(argName.getValue());
            if(argType instanceof PointerInfo p && p.isFunctionPointer()){
                writer.write(p.getFunctionPointerDeclaration(argNameCID));
            }else{
                writer.write(argType.getCname());
                writer.write(' ');
                writer.write(argNameCID);
            }
            if(env.hasFunctionInCurrentEnv(argName.getValue())) {
                throw new CompilerException(argName,"there is already argument called " + argName);
            }
            env.addFunction(argName.getValue(), new Variable(argType,IFunction.toCId(argName.getValue()),argName.getValue()));
        }
        writer.write(')');
        var array = new TypeInfo[args.size()];
        args.toArray(array);
        return array;
    }
}