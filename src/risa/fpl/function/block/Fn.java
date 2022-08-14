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
        var templateArgs = it.checkTemplate()?parseTemplateArguments(it,fnEnv):null;
		var id = it.nextID();
        if(env instanceof ModuleEnv e && e.isMain() && id.getValue().equals("main")){
           error(id,"main function can only be declared using built-in function main");
        }
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)){
	    	cID = id.getValue();
	    	if(IFunction.notCID(id.getValue())){
	    		error(id,"invalid C identifier");
	    	}
	    }else{
	    	cID = (!env.hasModifier(Modifier.ABSTRACT) && env instanceof ANameSpacedEnv tmp?tmp.getNameSpace(this):"") + IFunction.toCId(id.getValue());
	    }
        var self = env instanceof ClassEnv cEnv?cEnv.getInstanceInfo():(env instanceof InterfaceEnv e?e.getType():null);
        var argsBuilder = new StringBuilder();
        Function f;
        FunctionType type;
        var implName = cID;
        if(env.hasModifier(Modifier.ABSTRACT)){
            type = FunctionType.ABSTRACT;
            appendSemicolon = false;
            if(env instanceof ClassEnv e && e.notAbstract()){
                error(line,tokenNum,"abstract method can only be declared in abstract class");
            }
            if(env.getAccessModifier() == AccessModifier.PRIVATE){
                error(line,tokenNum,"abstract function can't be private");
            }
        }else if(env.hasModifier(Modifier.VIRTUAL) || env.hasModifier(Modifier.OVERRIDE)){
            type = FunctionType.VIRTUAL;
            implName = IFunction.toCId(id.getValue());
        }else{
            type = env.hasModifier(Modifier.NATIVE)?FunctionType.NATIVE:FunctionType.NORMAL;
        }
        if(env.hasFunctionInCurrentEnv(id.getValue())){
            if(env.getFunction(id) instanceof Function ft){
                f = ft;
                if(f.getType() != type){
                    error(line,tokenNum,"all variants require same function type");
                }
                if(f.getAccessModifier() != env.getAccessModifier()){
                    error(line,tokenNum,"all variants require same access modifier");
                }
            }else{
                throw new CompilerException(line,tokenNum,"there is already a function called " + id);
            }
        }else{
            f = new Function(id.getValue(),returnType,type,self,env.getAccessModifier());
        }
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
                       error(attr,"attribute duplicity");
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
        var argsArray = args.values().toArray(new TypeInfo[0]);
        FunctionVariant variant = null;
        if(f.hasVariant(argsArray) && (variant = f.getVariant(argsArray)).getLine() != line){
            error(line,tokenNum,"this function already has variant with arguments " + Arrays.toString(argsArray));
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
        var b = new StringBuilder();
		if(it.hasNext()){
		    if(env.hasModifier(Modifier.ABSTRACT)){
		        error(line,tokenNum,"abstract methods can only be declared");
            }
            var codeExp = it.next();
            if(codeExp instanceof Atom a){
                if(!a.getValue().equals("=")){
                    error(a,"= expected");
                }
                oneLine = true;
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
                }else if(arg.getValue() instanceof InterfaceInfo){
                    fnEnv.addInterfaceFreeCall(IFunction.toCId(arg.getKey()));
                }
            }
			var code = new StringBuilder();
			var fReturnType = codeExp.compile(code,fnEnv,it);
            if(oneLine && returnType != TypeInfo.VOID){
                b.append("return ");
                //noinspection ConstantConditions
                var a = (Atom)(codeExp instanceof List l?l.getExps().get(0):codeExp);
                if(returnType instanceof InstanceInfo i && i.getCopyConstructorName() != null && a.getType() != AtomType.STRING){
                    b.append(i.getCopyConstructorName()).append("AndReturn(").append(code).append(')');
                }else{
                    b.append(code);
                }
            }else{
                b.append(code);
            }
			if(oneLine && fnEnv.getReturnType() != TypeInfo.VOID && !fReturnType.equals(fnEnv.getReturnType())){
			    throw new CompilerException(codeExp,fReturnType + " cannot be implicitly converted to " + fnEnv.getReturnType());
            }
		    if(oneLine){
                b.append(";\n");
            }
            if(returnType == TypeInfo.VOID){
                fnEnv.compileDestructorCalls(b);
            }
			if(fnEnv.isReturnNotUsed() && returnType != TypeInfo.VOID){
				error(codeExp,"there is no return in this function and this function doesn't return void");
			}
            b.append("}\n");
		}else{
		    if(!(env.hasModifier(Modifier.ABSTRACT) || env.hasModifier(Modifier.NATIVE))){
		        error(line,tokenNum,"block required");
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
                    error(line,tokenNum,"this method doesn't have signature of one it overrides");
                }
            }
        }
        if(templateArgs == null){
            var headBuilder = new StringBuilder(attrCode);
            if(env.getAccessModifier() == AccessModifier.PRIVATE && !(env instanceof ClassEnv)){
                headBuilder.append("static ");
            }
            headBuilder.append(returnType.getCname()).append(' ').append(variant.getCname()).append(argsBuilder);
            if(env instanceof ClassEnv cEnv){
                var tmp = new StringBuilder(headBuilder);
                fnEnv.compileToPointerVars(tmp);
                tmp.append(b);
                cEnv.addMethod(f,tmp.toString());
            }else if(env instanceof InterfaceEnv){
                builder.append(p.getPointerVariableDeclaration(variant.getCname()));
            }else if(env instanceof ModuleEnv e){
                if(f.getType() != FunctionType.NATIVE){
                    var tmp = new StringBuilder(headBuilder);
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