package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class Var implements IFunction{
    private final TypeInfo type;
    public Var(TypeInfo type){
    	this.type = type;
    }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        if(type != null && it.hasNext()){
           if(it.peek() instanceof Atom a && a.getType() == TokenType.CLASS_SELECTOR){
               it.next();
               return type.getClassInfo();
           }
        }
		if(env.hasModifier(Modifier.NATIVE) && env instanceof ClassEnv){
            throw new CompilerException(line,charNum,"native variables can only be declared in modules");
		}
		var decType = type;
		if(it.checkTemplate()){
		    if(type instanceof TemplateTypeInfo tType){
		        decType = tType.generateTypeFor(IFunction.parseTemplateGeneration(it,env),env);
            }else if(type instanceof PointerInfo p && p.getType() instanceof TemplateTypeInfo tType){
                decType = new PointerInfo(tType.generateTypeFor(IFunction.parseTemplateGeneration(it,env),env));
            }else{
		        throw new CompilerException(line,charNum,"template type expected instead of " + type);
            }
        }
		while(it.hasNext()){
            var id = it.nextAtom();
            if(id.getType() == TokenType.ID){
                var cID = IFunction.toCId(id.getValue());
                if(env instanceof ModuleEnv e){
                    cID = e.getNameSpace() + cID;
                }
                var onlyDeclared = false;
                TypeInfo expType = null;
                var expCode = "";
                if(it.hasNext()){
                    var exp = it.next();
                    if(exp instanceof Atom a && a.getType() == TokenType.END_ARGS){
                        onlyDeclared = true;
                        if(env.hasModifier(Modifier.CONST) && !(env instanceof  ClassEnv)){
                            throw new CompilerException(id,"constant has to be defined");
                        }
                        if(decType == null){
                            throw new CompilerException(exp,"definition required");
                        }
                    }else{
                        if(env.hasModifier(Modifier.NATIVE)){
                            throw new CompilerException(exp,"native variables can only be declared");
                        }
                        var buffer = new BuilderWriter(writer);
                        expType = exp.compile(buffer,env,it);
                        expCode = buffer.getCode();
                    }
                }else{
                    if(env.hasModifier(Modifier.CONST) && !(env instanceof ClassEnv)){
                        throw new CompilerException(id,"constant has to be defined");
                    }
                    if(decType == null){
                        throw new CompilerException(id,"definition required");
                    }
                    onlyDeclared = true;
                }
                TypeInfo instanceType = null;
                if(env instanceof ClassEnv e){
                    instanceType = e.getInstanceType();
                }
                if(env instanceof ModuleEnv || env instanceof ClassEnv){
                    onlyDeclared = false;
                }
                var varType = Objects.requireNonNullElse(decType,expType);
                if(!varType.isPrimitive()){
                    onlyDeclared = false;
                }
                var decl = "";
                if(env.hasModifier(Modifier.NATIVE)){
                    decl = "extern ";
                }
                if((env instanceof  ClassEnv || env instanceof CStructEnv) && varType instanceof PointerInfo p && !p.getType().isPrimitive()){
                   decl += "struct ";
                }
                if(varType instanceof PointerInfo p && p.isFunctionPointer()){
                  decl += p.getFunctionPointerDeclaration(cID);
                }else{
                    decl += varType.getCname() + " " + cID;
                }
                if(env instanceof ModuleEnv mod){
                    mod.appendVariableDeclaration(decl + ";\n");
                }else{
                    writer.write(decl);
                }
                if(expType != null){
                    expCode = expType.ensureCast(this.type,expCode);
                    if(env instanceof ModuleEnv e){
                        e.appendToInitializer(cID + "=" + expCode + ";\n");
                    }else if(env instanceof ClassEnv e){
                        e.appendToImplicitConstructor("this->" + cID + "=" + expCode + ";\n");
                    }else{
                        writer.write('=');
                        writer.write(expCode);
                    }
                }
                writer.write(";\n");
                env.addFunction(id.getValue(),new Variable(varType,cID,onlyDeclared,id.getValue(),env.hasModifier(Modifier.CONST),instanceType,env.getAccessModifier()));
            }else if(id.getType() != TokenType.END_ARGS){
                throw new CompilerException(id,"unexpected atom");
            }
		}
		return TypeInfo.VOID;
	}
}