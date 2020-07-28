package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.Modifier;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class Var implements IFunction {
    private final TypeInfo type;
    public Var(TypeInfo type) {
    	this.type = type;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		if(env.hasModifier(Modifier.NATIVE)) {
			writer.write("extern ");
		}
		if(type != null && !(type instanceof Function)) {
			if(env.hasModifier(Modifier.CONST)) {
				writer.write("const ");
			}
			writer.write(type.cname);
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
				cID = id.value;
				if(!IFunction.isCId(cID)) {
					throw new CompilerException(id,cID + " is not valid C identifier");
				}
			}else {
				cID = "";
				if(env instanceof ModuleEnv mod) {
					cID = mod.getNameSpace();
				}
				cID += IFunction.toCId(id.value);
			}
			if(env.hasFunctionInCurrentEnv(id.value)) {
				throw new CompilerException(id,"there is already variable or function called " + id);
			}
			var declaredOnly = true;
			if(type != null) {
			    if(type instanceof Function f){
			       cID = createFunctionPointer(cID,f);
                }
				writer.write(cID);
			}
            var type = this.type;
			if(it.hasNext()) {
				if(env.hasModifier(Modifier.NATIVE)) {
					throw new CompilerException(id,"native variables can only be declared");
				}
				var exp = it.nextAtom();
				if(exp.type == TokenType.ARG_SEPARATOR) {
					if(type == null) {
						throw new CompilerException(exp,"cannot infer type");
					}
				}else {
					declaredOnly = false;
					var b = new BuilderWriter(writer);
					b.write('=');
					var expType  = env.getFunction(exp).compile(b, env, it, line, charNum);
					if(type != null && !type.equals(expType)) {
						throw new CompilerException(exp,expType  + " cannot be implicitly converted to " + type);
					}
					if(type == null) {
						if(env.hasModifier(Modifier.CONST)) {
							writer.write("const ");
						}
						if(expType  instanceof Function f){
						    cID = createFunctionPointer(cID,f);
                        }else{
                            writer.write(expType.cname);
                            writer.write(' ');
                        }
						writer.write(cID);
					}
					type = expType;
					writer.write(b.getText());
				}
			}else if(type == null) {
				throw new CompilerException(id,"cannot infer type");
			}else {
				if(env instanceof ClassEnv || env instanceof ModuleEnv) {
					declaredOnly = false;
				}
			}
			env.addFunction(id.value, new Variable(type,cID,declaredOnly,id.value,env.hasModifier(Modifier.CONST)));
		}
		return TypeInfo.VOID;
	}
	private String createFunctionPointer(String cID,Function f){
        var b = new StringBuilder(f.returnType.cname);
        b.append("(*");
        b.append(cID);
        b.append(")(");
        var firstArg = true;
        for(var arg:f.args){
            if(firstArg){
                firstArg = false;
            }else{
                b.append(',');
            }
            b.append(arg);
        }
        b.append(")");
        return b.toString();
    }
}