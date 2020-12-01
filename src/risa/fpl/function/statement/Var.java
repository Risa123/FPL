package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.IFunction;
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
        var bWriter = new BuilderWriter(writer);
        if(type != null && it.hasNext()){
           if(it.peek() instanceof Atom a && a.getType() == TokenType.CLASS_SELECTOR){
               it.next();
               return type.getClassInfo();
           }
        }
		while(it.hasNext()) {
            if(env.hasModifier(Modifier.NATIVE)) {
                bWriter.write("extern ");
            }
            if(type != null && !(type instanceof PointerInfo p && p.isFunctionPointer())) {
                if(env.hasModifier(Modifier.CONST) && !(env instanceof ANameSpacedEnv)) {
                    bWriter.write("const ");
                }
                if(env instanceof ClassEnv && (!type.isPrimitive() || type instanceof PointerInfo p && !p.getType().isPrimitive())){
                    bWriter.write("struct ");
                }
                bWriter.write(type.getCname());
                bWriter.write(' ');
            }
			var id = it.nextID();
			String cID;
			if(env.hasModifier(Modifier.NATIVE)) {
				cID = id.getValue();
				if(IFunction.notCID(cID)) {
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
			       bWriter.write(p.getFunctionPointerDeclaration(cID));
                }else{
                    bWriter.write(cID);
                }
			}
            var type = this.type;
			if(it.hasNext()) {
				var exp = it.nextAtom();
				if(exp.getType() == TokenType.END_ARGS) {
					if(type == null) {
						throw new CompilerException(exp,"cannot infer type");
					}
				}else {
                    if(env.hasModifier(Modifier.NATIVE)) {
                        throw new CompilerException(id,"native variables can only be declared");
                    }
                    if(env instanceof CStructEnv){
                        throw new CompilerException(id,"C structures can only contain declarations");
                    }
					declaredOnly = false;
					var b = new BuilderWriter(bWriter);
					b.write('=');
                    var list = new ArrayList<AExp>();
                    list.add(exp);
                    while(it.hasNext()){
                        var expPart = it.next();
                        if(expPart instanceof Atom a && a.getType() == TokenType.END_ARGS){
                            break;
                        }
                        list.add(expPart);
                    }
                    var buffer = new BuilderWriter(bWriter);
					var expType  = new List(exp.getLine(),exp.getCharNum(),list,true).compile(buffer,env,it);
					if(type != null){
                        if(!type.equals(expType)) {
                            throw new CompilerException(exp,expType  + " cannot be implicitly converted to " + type);
                        }
                        b.write(expType.ensureCast(type,buffer.getCode()));
                    }else{
					    b.write(buffer.getCode());
                    }
					if(type == null) {
						if(env.hasModifier(Modifier.CONST) && !(env instanceof  ANameSpacedEnv)) {
							bWriter.write("const ");
						}
						if(expType  instanceof PointerInfo p && p.isFunctionPointer()){
                            bWriter.write(p.getFunctionPointerDeclaration(cID));
                        }else{
						    if(!expType.isPrimitive() || (expType instanceof  PointerInfo p && !p.getType().isPrimitive())){
						        bWriter.write("struct ");
                            }
                            bWriter.write(expType.getCname());
                            bWriter.write(' ');
                            bWriter.write(cID);
                        }
					}
					type = expType;
					if(env instanceof  ClassEnv e){
					    e.appendToImplicitConstructor("this->" + cID + b.getCode() + ";\n");
                    }else if(env instanceof  ModuleEnv e){
                       e.appendToInitializer(cID + b.getCode() + ";\n");
					}else{
                        bWriter.write(b.getCode());
                    }
				}
			}else if(type == null) {
				throw new CompilerException(id,"cannot infer type");
			}
            if(env instanceof ClassEnv || env instanceof ModuleEnv) {
                declaredOnly = false;
            }
            bWriter.write(";\n");
            TypeInfo instanceType = null;
            if(env instanceof ClassEnv e){
                instanceType = e.getInstanceType();
            }
            if(env instanceof ModuleEnv  e){
                e.appendVariableDeclaration(bWriter.getCode());
            }else{
                writer.write(bWriter.getCode());
            }
			var v = new Variable(type,cID,declaredOnly,id.getValue(),env.hasModifier(Modifier.CONST),instanceType,env.getAccessModifier());
			env.addFunction(id.getValue(),v);
		}
		return TypeInfo.VOID;
	}
}