package risa.fpl.function.block;

import java.util.ArrayList;
import java.util.Arrays;

import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.*;
import risa.fpl.info.*;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

public class Fn extends AFunctionBlock{
	private boolean appendSemicolon;
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum,Modifier.NATIVE,Modifier.VIRTUAL,Modifier.ABSTRACT,Modifier.OVERRIDE);
		var returnType = env.getType(it.nextID());
        var fnEnv = new FnEnv(env,returnType);
        var templateArgs = it.checkTemplate()?IFunction.parseTemplateArguments(it,fnEnv):null;
        var b = new StringBuilder();
		var id = it.nextID();
        if(env instanceof ModuleEnv e && e.isMain() && id.getValue().equals("main")){
           throw new CompilerException(id,"main function can only be declared using built-in function main");
        }
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)){
	    	cID = id.getValue();
	    	if(IFunction.notCID(id.getValue())){
	    		throw new CompilerException(id,"invalid C identifier");
	    	}
	    }else{
	    	cID = !env.hasModifier(Modifier.ABSTRACT) && env instanceof ANameSpacedEnv tmp?tmp.getNameSpace(this):"";
            cID += IFunction.toCId(id.getValue());
	    }
        var self = env instanceof  ClassEnv cEnv?cEnv.getInstanceInfo():(env instanceof InterfaceEnv e?e.getType():null);
        var argsBuilder = new StringBuilder();
		var args = parseArguments(argsBuilder,it,fnEnv,self);
		var attrCode = new StringBuilder();
        if(it.hasNext() && it.peek() instanceof Atom a && a.getType() == AtomType.CLASS_SELECTOR){
            it.next();
            var attrs = new ArrayList<String>();
            if(it.hasNext()){
                attrCode.append("__attribute__((");
            }
            while(it.hasNext()){
               if(it.peek() instanceof List || it.peek() instanceof Atom atom && atom.getValue().equals("=")){
                   break;
               }else{
                   var attr = it.nextID();
                   if(attrs.contains(attr.getValue())){
                       throw new CompilerException(attr,"attribute duplicity");
                   }
                   attrs.add(attr.getValue());
                   switch(attr.getValue()){
                       case "noReturn"->{
                           if(attrs.contains("returnsTwice")){
                               throw new CompilerException(attr,"noReturn is mutually exclusive with returnsTwice");
                           }
                           attrCode.append("__noreturn__");
                       }
                       case "returnsTwice"->{
                           if(attrs.contains("noReturn")){
                               throw new CompilerException(attr,"returnsTwice is mutually exclusive with noReturn");
                           }
                           attrCode.append("__returns_twice__");
                       }
                       default->throw new CompilerException(attr,"no attribute is called " + attr);
                   }
               }
            }
            if(attrCode.length() > 0){
                attrCode.append("))");
            }
        }
        var oneLine = false;
        Function f;
        FunctionType type;
        var implName = cID;
        if(env.hasModifier(Modifier.ABSTRACT)){
            type = FunctionType.ABSTRACT;
            appendSemicolon = false;
            if(env instanceof ClassEnv e && e.notAbstract()){
                throw new CompilerException(line,tokenNum,"abstract method can only be declared in abstract class");
            }
            if(env.getAccessModifier() == AccessModifier.PRIVATE){
                throw new CompilerException(line,tokenNum,"abstract function can't be private");
            }
        }else if(env.hasModifier(Modifier.VIRTUAL) || env.hasModifier(Modifier.OVERRIDE)){
            type = FunctionType.VIRTUAL;
            implName = IFunction.toCId(id.getValue());
        }else if(env.hasModifier(Modifier.NATIVE)){
            type = FunctionType.NATIVE;
        }else{
            type = FunctionType.NORMAL;
        }
        if(env.hasFunctionInCurrentEnv(id.getValue())){
            if(env.getFunction(id) instanceof Function ft){
                f = ft;
                if(f.getType() != type){
                    throw new CompilerException(line,tokenNum,"all variants require same function type");
                }
                if(f.getAccessModifier() != env.getAccessModifier()){
                    throw new CompilerException(line,tokenNum,"all variants require same access modifier");
                }
            }else{
                throw new CompilerException(line,tokenNum,"there is already a function called " + id);
            }
        }else{
            f = new Function(id.getValue(),returnType,type,self,env.getAccessModifier(),attrCode.toString());
        }
        var argsArray = args.values().toArray(new TypeInfo[0]);
        FunctionVariant variant = null;
        if(f.hasVariant(argsArray) && (variant = f.getVariant(argsArray)).getLine() != line){
            throw new CompilerException(line,tokenNum,"this function already has variant with arguments " + Arrays.toString(argsArray));
        }
        FunctionInfo p = null;
        if(templateArgs == null){
            if(variant == null){
                variant = f.addVariant(argsArray,cID,implName);
                variant.setLine(line);
            }
            p = new FunctionInfo(f);
            fnEnv.addFunction("&" + id,new FunctionReference(p));
            fnEnv.addType(p,false);
        }
        fnEnv.addFunction(id.getValue(),f);
		if(it.hasNext()){
		    if(env.hasModifier(Modifier.ABSTRACT)){
		        throw new CompilerException(line,tokenNum,"abstract methods can only be declared");
            }
            var codeExp = it.next();
            if(codeExp instanceof Atom a){
                oneLine = true;
                if(!a.getValue().equals("=")){
                    throw new CompilerException(a,"= expected");
                }
                var atoms = new ArrayList<AExp>();
                while(it.hasNext()){
                    atoms.add(it.nextAtom());
                }
                codeExp = new List(line,a.getTokenNum(),atoms,false);
            }
            if(templateArgs != null){
                f.addTemplateVariant(templateArgs,codeExp,args,env,line);
            }
            argsBuilder.append("{\n");
            for(var arg:args.entrySet()){
                if(arg.getValue() instanceof InstanceInfo i){
                    fnEnv.addInstanceVariable(i,IFunction.toCId(arg.getKey()));
                }
            }
			if(oneLine && returnType != TypeInfo.VOID){
			    b.append("return ");
            }
			var code = new StringBuilder();
			var fReturnType = codeExp.compile(code,fnEnv,it);
			if(oneLine && fnEnv.getReturnType() != TypeInfo.VOID && !fReturnType.equals(fnEnv.getReturnType())){
			    throw new CompilerException(codeExp,fReturnType + " cannot be implicitly converted to " + fnEnv.getReturnType());
            }
			b.append(code);
		    if(oneLine){
                b.append(";\n");
            }
            if(returnType == TypeInfo.VOID){
                fnEnv.compileDestructorCalls(b);
            }
			if(fnEnv.isReturnNotUsed() && returnType != TypeInfo.VOID){
				throw new CompilerException(codeExp,"there is no return in this function and this function doesn't return void");
			}
            b.append("}\n");
		}else{
		    if(!(env.hasModifier(Modifier.ABSTRACT) || env.hasModifier(Modifier.NATIVE))){
		        throw new CompilerException(line,tokenNum,"block required");
            }
			appendSemicolon = true;
		}
        if(self != null){
            AField parentField = null;
            var parents = self.getParents();
            for(var parent:parents){
                parentField = parent.getField(id.getValue(),env);
                if(parentField != null){
                    break;
                }
            }
            if(env.hasModifier(Modifier.OVERRIDE)){
                if(!(parentField instanceof Function parentMethod)){
                    throw new CompilerException(line,tokenNum,"there is no method " + id + " to override");
                }
                if(!parentMethod.hasSignature(f)){
                    throw new CompilerException(line,tokenNum,"this method doesn't have signature of one it overrides");
                }
            }else if(parentField != null){
                throw new CompilerException(line,tokenNum,"override is required");
            }
        }
        if(templateArgs == null){
            var headBuilder = new StringBuilder(attrCode);
            if(env.getAccessModifier() == AccessModifier.PRIVATE && !(env instanceof ClassEnv)){
                headBuilder.append("static ");
            }
            headBuilder.append(returnType.getCname()).append(' ').append(variant.getCname()).append(argsBuilder);
            if(env instanceof ClassEnv cEnv){
                var tmp = new StringBuilder();
                tmp.append(headBuilder);
                fnEnv.compileToPointerVars(tmp);
                tmp.append(b);
                cEnv.addMethod(f,tmp.toString());
            }else if(env instanceof InterfaceEnv){
                builder.append(p.getPointerVariableDeclaration(variant.getCname()));
            }else if(env instanceof ModuleEnv e){
                if(f.getType() != FunctionType.NATIVE){
                    var tmp = new StringBuilder();
                    tmp.append(headBuilder);
                    fnEnv.compileToPointerVars(tmp);
                    tmp.append(b);
                    e.appendFunctionCode(tmp.toString());
                }
                e.appendFunctionDeclaration(f);
            }else{
                fnEnv.compileToPointerVars(builder);
                builder.append(b);
                if(type == FunctionType.NATIVE && !id.getValue().equals("__builtin_longjmp")){//builtin functions cannot have declaration with extern
                    builder.append(f.getDeclaration());
                }
            }
        }
        if(templateArgs == null){
            env.addFunction("&" + id,new FunctionReference(p));
            env.addType(p,false);
        }
        if(type == FunctionType.NATIVE && env instanceof FnSubEnv){
            appendSemicolon = false;
        }
        env.addFunction(id.getValue(),f);
		return TypeInfo.VOID;
	}
	@Override
	public boolean appendSemicolon(){
		return appendSemicolon;
	}
}