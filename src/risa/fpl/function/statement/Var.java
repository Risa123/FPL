package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public final class Var implements IFunction {
    private final TypeInfo type;
    public Var(TypeInfo type) {
    	this.type = type;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        if(type != null && it.hasNext()){
           var next = it.peek();
           if(next instanceof Atom a && a.getType() == TokenType.CLASS_SELECTOR){
               it.next();
               return type.getClassInfo();
           }
        }
		if(env.hasModifier(Modifier.NATIVE)) {
			writer.write("extern ");
		}
		if(type != null && !(type instanceof Function)) {
			if(env.hasModifier(Modifier.CONST)) {
				writer.write("const ");
			}
			writer.write(type.getCname());
			writer.write(' ');
		}
		var first = true;
		while(it.hasNext()) {
			if(first) {
				first = false;
			}else {
				if(type == null) {
					writer.write(';');
				}else {
					writer.write(',');
				}
			}
			var id = it.nextID();
			String cID;
			if(env.hasModifier(Modifier.NATIVE)) {
				cID = id.getValue();
				if(!IFunction.isCId(cID)) {
					throw new CompilerException(id,cID + " is not valid C identifier");
				}
			}else {
				cID = "";
				if(env instanceof ANameSpacedEnv tmp) {
					cID = tmp.getNameSpace(this);
				}
				cID += IFunction.toCId(id.getValue());
			}
			if(env.hasFunctionInCurrentEnv(id.getValue())) {
				throw new CompilerException(id,"there is already variable or function called " + id);
			}
			var declaredOnly = true;
			if(type != null) {
			    if(type instanceof PointerInfo p && p.isFunctionPointer()){
			       cID = p.getFunctionPointerDeclaration(cID);
                }
				writer.write(cID);
			}
            var type = this.type;
			if(it.hasNext()) {
				if(env.hasModifier(Modifier.NATIVE)) {
					throw new CompilerException(id,"native variables can only be declared");
				}
				var exp = it.nextAtom();
				if(exp.getType() == TokenType.ARG_SEPARATOR) {
					if(type == null) {
						throw new CompilerException(exp,"cannot infer type");
					}
				}else {
					declaredOnly = false;
					var b = new BuilderWriter(writer);
					b.write('=');
                    var list = new ArrayList<AExp>();
                    list.add(exp);
                    while(it.hasNext()){
                        var expPart = it.next();
                        if(expPart instanceof Atom a && a.getType() == TokenType.ARG_SEPARATOR){
                            break;
                        }
                        list.add(expPart);
                    }
					var expType  = new List(exp.getLine(),exp.getCharNum(),list,true).compile(b,env,it);
					if(type != null && !type.equals(expType)) {
						throw new CompilerException(exp,expType  + " cannot be implicitly converted to " + type);
					}
					if(type == null) {
						if(env.hasModifier(Modifier.CONST)) {
							writer.write("const ");
						}
						if(expType  instanceof PointerInfo p && p.isFunctionPointer()){
						    cID = p.getFunctionPointerDeclaration(cID);
                        }else{
                            writer.write(expType.getCname());
                            writer.write(' ');
                        }
						writer.write(cID);
					}
					type = expType;
					if(env instanceof  ClassEnv e){
					    e.appendToDefaultConstructor("this->" + cID + b.getText() + ";\n");
                    }else{
                        writer.write(b.getText());
                    }
				}
			}else if(type == null) {
				throw new CompilerException(id,"cannot infer type");
			}else {
				if(env instanceof ClassEnv || env instanceof ModuleEnv) {
					declaredOnly = false;
				}
			}
			env.addFunction(id.getValue(), new Variable(type,cID,declaredOnly,id.getValue(),env.hasModifier(Modifier.CONST),env instanceof ClassEnv));
		}
		return TypeInfo.VOID;
	}

}