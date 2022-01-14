package risa.fpl.function.statement;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

@SuppressWarnings("ClassCanBeRecord")
public final class Var implements IFunction{
    private final TypeInfo type;
    public Var(TypeInfo type){
    	this.type = type;
    }
	@SuppressWarnings("ConstantConditions")
    @Override
	public TypeInfo compile(StringBuilder  builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum,Modifier.NATIVE,Modifier.CONST);
        if(type != null && it.hasNext()){
           if(it.peek() instanceof Atom a && a.getType() == AtomType.CLASS_SELECTOR){
               it.next();
               var c = type.getClassInfo();
               if(it.peek() instanceof Atom a1 && a1.getType() == AtomType.ID){
                   it.next();
                   var field = c.getField(a1.getValue(),env);
                   if(field == null){
                       throw new CompilerException(a1,c + " has no field called " + a1);
                   }
                   return field.compile(builder,env,it,a1.getLine(),a1.getTokenNum());
               }
               return c;
           }
        }
		if(env.hasModifier(Modifier.NATIVE) && env instanceof ClassEnv){
            throw new CompilerException(line,tokenNum,"native variables can only be declared in modules");
		}
		var decType = type;
		try{
            if(it.checkTemplate()){
                if(type instanceof TemplateTypeInfo tType){
                    decType = tType.generateTypeFor(IFunction.parseTemplateGeneration(it,env),env,it.getLastLine(),it.getLastCharNum());
                }else if(type instanceof PointerInfo p && p.getType() instanceof TemplateTypeInfo tType){
                    decType = new PointerInfo(tType.generateTypeFor(IFunction.parseTemplateGeneration(it,env),env,it.getLastLine(),it.getLastCharNum()));
                }else{
                    throw new CompilerException(line,tokenNum,"template type expected instead of " + type);
                }
            }
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
		while(it.hasNext()){
            var id = it.nextAtom();
            if(env instanceof ClassEnv e && !e.getVariableFieldDeclarationOrder().contains(id.getValue()) && !id.getValue().equals(";")){
                e.getVariableFieldDeclarationOrder().add(id.getValue());
            }
            if(id.getType() == AtomType.ID){
                String cID;
                if(env.hasModifier(Modifier.NATIVE)){
                    cID = id.getValue();
                    if(IFunction.notCID(cID)){
                        throw new CompilerException(line,tokenNum,cID + " is not a valid C identifier");
                    }
                }else{
                    cID = IFunction.toCId(id.getValue());
                    if(env instanceof ModuleEnv e && env.getAccessModifier() != AccessModifier.PRIVATE){
                        cID = e.getNameSpace() + cID;
                    }
                }
                if(env.hasFunctionInCurrentEnv(id.getValue())){
                    throw new CompilerException(id,"there is already a function called " + id);
                }
                var onlyDeclared = false;
                TypeInfo expType = null;
                var expCode = "";
                var constantExp = false;
                if(it.hasNext()){
                    var exp = it.next();
                    if(exp instanceof Atom a && a.getType() == AtomType.END_ARGS){
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
                        var buffer = new StringBuilder();
                        if(!it.hasNext() || it.peek() instanceof Atom p && p.getType() == AtomType.END_ARGS && exp instanceof Atom a && a.getType() != AtomType.ID){
                            if(exp instanceof Atom a && a.getType() != AtomType.STRING){
                                constantExp = true;
                            }
                        }
                        expType = exp.compile(buffer,env,it);
                        expCode = buffer.toString();
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
                    instanceType = e.getInstanceInfo();
                }
                if(env instanceof ANameSpacedEnv){
                    onlyDeclared = false;
                }
                var varType = Objects.requireNonNullElse(decType,expType);
                if(!varType.isPrimitive()){
                    onlyDeclared = false;
                }
                var declaration = "";
                if(env.hasModifier(Modifier.NATIVE)){
                    declaration = "extern ";
                }
                if(env instanceof  ClassEnv && varType instanceof PointerInfo p && !p.getType().isPrimitive()){
                   declaration += "struct ";
                }
                declaration += varType.getCname() + " " + cID;
                if(env.getAccessModifier() == AccessModifier.PRIVATE && !env.hasModifier(Modifier.NATIVE) && env instanceof ModuleEnv){
                    declaration = "static " + declaration;
                }
                if(env instanceof ModuleEnv mod){
                    mod.appendVariableDeclaration(declaration);
                    if(!constantExp){
                        mod.appendVariableDeclaration(";\n");
                    }
                }else{
                    builder.append(declaration);
                }
                if(expType != null){
                    expCode = expType.ensureCast(this.type,expCode);
                    if(env instanceof ModuleEnv e){
                       if(constantExp){
                           e.appendVariableDeclaration("=" + expCode + ";\n");
                       }else{
                           e.appendToInitializer(cID + "=" + expCode + ";\n");
                       }
                    }else if(env instanceof ClassEnv e){
                        e.appendToImplicitConstructor("this->" + cID + "=" + expCode + ";\n");
                    }else{
                        builder.append('=').append(expCode);
                    }
                }
                if(it.hasNext()){
                    builder.append(";\n");
                }
                var constant = env.hasModifier(Modifier.CONST);
                if(env.hasModifier(Modifier.NATIVE)){
                    onlyDeclared = false;
                }
                env.addFunction(id.getValue(),new Variable(varType,cID,onlyDeclared,id.getValue(),constant,instanceType,env.getAccessModifier()));
            }else if(id.getType() != AtomType.END_ARGS){
                throw new CompilerException(id,"expected ;");
            }
		}
		return TypeInfo.VOID;
	}
}