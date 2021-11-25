package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import risa.fpl.BuilderWriter;
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
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
		var returnType = env.getType(it.nextID());
        var fnEnv = new FnEnv(env,returnType);
        LinkedHashMap<String,TypeInfo> templateArgs = null;
        if(it.checkTemplate()){
            templateArgs = IFunction.parseTemplateArguments(it,fnEnv);
        }
        var b = new BuilderWriter();
		var id = it.nextID();
        if(env instanceof  ModuleEnv e && e.isMain() && id.getValue().equals("main")){
           throw new CompilerException(id,"main function can only be declared using built-in function main");
        }
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)){
	    	cID = id.getValue();
	    	if(IFunction.notCID(id.getValue())){
	    		throw new CompilerException(id,"invalid C identifier");
	    	}
	    }else{
	    	cID = "";
	    	if(!env.hasModifier(Modifier.ABSTRACT) && env instanceof ANameSpacedEnv tmp){
	    	    cID = tmp.getNameSpace(this);
	    	}
	    	cID += IFunction.toCId(id.getValue());
	    }
        TypeInfo self = null;
        if(env instanceof  ClassEnv cEnv){
            self = cEnv.getInstanceType();
        }else if(env instanceof InterfaceEnv e){
            self = e.getType();
        }
        var headWriter = new BuilderWriter();
        if(env.getAccessModifier() == AccessModifier.PRIVATE && !(env instanceof ClassEnv)){
            headWriter.write("static ");
        }
        FunctionInfo fPointer = null;
        if(returnType instanceof IPointerInfo p){
            fPointer = p.getFunctionPointer();
        }
        if(fPointer != null){
            headWriter.write(fPointer.getFunction().getReturnType().getCname());
            headWriter.write('(');
            headWriter.write("*".repeat(((IPointerInfo)returnType).getFunctionPointerDepth() + 1));
        }else{
            headWriter.write(returnType.getCname());
        }
        headWriter.write(' ' + cID);
        if(!env.hasModifier(Modifier.NATIVE)){
           if(env.hasFunctionInCurrentEnv(id.getValue()) && env.getFunction(id) instanceof  Function f){
               headWriter.write(Integer.toString(f.getVariants().size()));
           }else{
               headWriter.write('0');
           }
        }
		var args = parseArguments(headWriter,it,fnEnv,self);
        if(fPointer != null){
            headWriter.write(")(");
            for(var arg:args.entrySet()){
                if(arg.getValue() instanceof IPointerInfo p){
                    headWriter.write(p.getPointerVariableDeclaration(arg.getKey()));
                }else{
                    headWriter.write(arg.getValue().getCname() + " " + arg.getKey());
                }
            }
            headWriter.write(')');
        }
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
            if(env instanceof ClassEnv e && !e.isAbstract()){
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
        if(f.hasVariant(argsArray)){
            throw new CompilerException(line,tokenNum,"this function already has variant with arguments " + Arrays.toString(argsArray));
        }
        FunctionInfo p = null;
        FunctionVariant variant = null;
        if(templateArgs == null){
            variant = f.addVariant(argsArray,cID,implName);
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
                f.addTemplateVariant(templateArgs,codeExp,args,env);
            }
            headWriter.write("{\n");
            for(var arg:args.entrySet()){
                if(arg.getValue() instanceof InstanceInfo i){
                    fnEnv.addInstanceVariable(i,IFunction.toCId(arg.getKey()));
                }
            }
			if(oneLine && returnType != TypeInfo.VOID){
			    b.write("return ");
            }
			var code = new BuilderWriter();
			var fReturnType = codeExp.compile(code,fnEnv,it);
			if(oneLine && fnEnv.getReturnType() != TypeInfo.VOID && !fReturnType.equals(fnEnv.getReturnType())){
			    throw new CompilerException(codeExp,fReturnType + " cannot be implicitly converted to " + fnEnv.getReturnType());
            }
			b.write(code.getCode());
		    if(oneLine){
                b.write(";\n");
            }
            if(returnType == TypeInfo.VOID){
                fnEnv.compileDestructorCalls(b);
            }
			if(fnEnv.isReturnNotUsed() && returnType != TypeInfo.VOID){
				throw new CompilerException(codeExp,"there is no return in this function and this function doesn't return void");
			}
            b.write("}\n");
		}else{
		    if(!(env.hasModifier(Modifier.ABSTRACT) || env.hasModifier(Modifier.NATIVE))){
		        throw new CompilerException(line,tokenNum,"block required");
            }
			appendSemicolon = true;
		}
        var array = args.values().toArray(new TypeInfo[0]);
        if(self != null){
            IField parentField = null;
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
            if((env.hasModifier(Modifier.OVERRIDE) || env.hasModifier(Modifier.VIRTUAL)) && templateArgs == null){
                String methodImplName;
                if(env.hasModifier(Modifier.VIRTUAL)){
                    methodImplName = variant.implName();
                }else{
                    methodImplName = ((Function)parentField).getVariant(array).implName();
                }
                var cEnv = (ClassEnv)env;//override can only appear in ClassEnv
                cEnv.appendToInitializer(cEnv.getDataName() + "." + methodImplName + "=&");
                cEnv.appendToInitializer(variant.cname() + ";\n");
            }
        }
        if(templateArgs == null){
            if(env instanceof ClassEnv cEnv){
                var tmp = new BuilderWriter();
                tmp.write(attrCode.toString());
                tmp.write(headWriter.getCode());
                fnEnv.compileToPointerVars(tmp);
                tmp.write(b.getCode());
                cEnv.addMethod(f,argsArray,tmp.getCode());
            }else if(env instanceof InterfaceEnv){
                writer.write(p.getPointerVariableDeclaration(variant.cname()));
            }else if(env instanceof ModuleEnv e){
                if(f.getType() != FunctionType.NATIVE){
                    var tmp = new BuilderWriter();
                    tmp.write(attrCode.toString());
                    tmp.write(headWriter.getCode());
                    fnEnv.compileToPointerVars(tmp);
                    tmp.write(b.getCode());
                    e.appendFunctionCode(tmp.getCode());
                }
                e.appendFunctionDeclaration(f);
            }else{
                writer.write(attrCode.toString());
                fnEnv.compileToPointerVars(writer);
                writer.write(b.getCode());
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